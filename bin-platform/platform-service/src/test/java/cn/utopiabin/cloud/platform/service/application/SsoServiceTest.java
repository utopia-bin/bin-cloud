package cn.utopiabin.cloud.platform.service.application;

import cn.utopiabin.cloud.common.context.UserContextHolder;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;
import cn.utopiabin.cloud.platform.util.JwtTokenService;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SsoServiceTest {
    ApplicationFixture f;
    SsoService sso;
    long instance;
    String platformToken;
    final String secret="test-only-backend-secret";
    final String callback="http://localhost:5173/api/open/workbench/callback";
    final String verifier="a".repeat(43);
    @BeforeEach void setup() throws Exception {
        f=new ApplicationFixture();instance=f.provision(10,100);
        f.jdbc.update("UPDATE sys_application SET client_secret_hash=? WHERE id=2",SsoCrypto.hash(secret));
        var tickets=mock(SsoTicketStore.class);
        var codes=new ConcurrentHashMap<String,String>();
        doAnswer(i->{codes.put(i.getArgument(0),i.getArgument(1));return null;}).when(tickets).put(anyString(),anyString());
        when(tickets.consume(anyString())).thenAnswer(i->codes.remove(i.<String>getArgument(0)));
        var config=new JwtTokenProperties();config.setJwtSecret("test-only-0123456789abcdef0123456789abcdef");config.setJwtExpiration(7200);
        sso=new SsoService(f.store,f.boundary,tickets,new JwtTokenService(config),config,f.audit);
        var user=new SysUser();user.setId(100L);user.setTenantId(10L);user.setUsername("alice");
        platformToken=f.transaction.execute(s->sso.platformLogin(user,List.of()));
    }
    @AfterEach void clear() { UserContextHolder.clear(); }
    SsoExchangeDTO code() {
        var request=new SsoAuthorizeDTO();request.setTenantApplicationId(instance);request.setRedirectUri(callback);request.setState("s".repeat(32));request.setCodeChallenge(SsoCrypto.challenge(verifier));
        var issued=sso.authorize(platformToken,request);
        String code=URI.create(issued.getRedirectUrl()).getRawQuery().split("&")[0].substring(5);
        var dto=new SsoExchangeDTO();dto.setCode(code);dto.setClientId("open-api");dto.setClientSecret(secret);dto.setRedirectUri(callback);dto.setCodeVerifier(verifier);return dto;
    }
    SsoTokenVO exchange(SsoExchangeDTO dto) { return f.transaction.execute(s->sso.exchange(dto)); }
    @Test void ssoReturnsOnlyApplicationRolesAndPermissionsAndConsumesCodeOnce() {
        var request=code();var token=exchange(request);
        var profile=sso.profile(token.getAccessToken(),"learning-workbench");
        assertThat(profile.getRoles()).containsExactly("app_admin");
        assertThat(profile.getPermissionCodes()).containsExactlyInAnyOrder("workbench:read","workbench:execute").doesNotContain("*");
        assertThat(profile.getMenus()).hasSize(1);
        assertThatThrownBy(()->exchange(request)).isInstanceOf(BizException.class).hasMessageContaining("已经使用");
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM sys_sso_login_log WHERE success=0",Integer.class)).isEqualTo(1);
    }
    @Test void wrongClientDoesNotConsumeTicketButWrongPkceDoes() {
        var dto=code();dto.setClientSecret("wrong");
        assertThatThrownBy(()->exchange(dto)).isInstanceOf(BizException.class);
        dto.setClientSecret(secret);dto.setCodeVerifier("b".repeat(43));
        assertThatThrownBy(()->exchange(dto)).isInstanceOf(BizException.class).hasMessageContaining("PKCE");
        dto.setCodeVerifier(verifier);
        assertThatThrownBy(()->exchange(dto)).isInstanceOf(BizException.class).hasMessageContaining("已经使用");
    }
    @Test void callbackIsCaseSensitiveAndMustStillBeAllowedAtExchange() {
        var request=new SsoAuthorizeDTO();request.setTenantApplicationId(instance);request.setRedirectUri(callback.replace("callback","Callback"));request.setState("s".repeat(32));request.setCodeChallenge(SsoCrypto.challenge(verifier));
        assertThatThrownBy(()->sso.authorize(platformToken,request)).isInstanceOf(BizException.class).hasMessageContaining("白名单");
        var dto=code();f.jdbc.update("UPDATE sys_application_redirect_uri SET available=0 WHERE application_id=2");
        assertThatThrownBy(()->exchange(dto)).isInstanceOf(BizException.class).hasMessageContaining("白名单");
    }
    @Test void audiencesCannotBeInterchanged() {
        var app=exchange(code());
        assertThatThrownBy(()->sso.active(platformToken,"learning-workbench")).isInstanceOf(BizException.class);
        assertThatThrownBy(()->sso.active(app.getAccessToken(),"platform-console")).isInstanceOf(BizException.class);
        assertThatThrownBy(()->sso.logout(app.getAccessToken(),true)).isInstanceOf(BizException.class);
    }
    @Test void passwordChangeInvalidatesBothApplicationAndPlatformSessions() {
        var app=exchange(code());f.jdbc.update("UPDATE sys_user SET credential_version=1 WHERE id=100");
        assertThatThrownBy(()->sso.active(platformToken,"platform-console")).isInstanceOf(BizException.class);
        assertThatThrownBy(()->sso.active(app.getAccessToken(),"learning-workbench")).isInstanceOf(BizException.class);
    }
    @Test void logoutOnlyApplicationKeepsPlatformAndGlobalLogoutRevokesChildren() {
        var first=exchange(code());f.transaction.executeWithoutResult(s->sso.logout(first.getAccessToken(),false));
        assertThat(sso.active(platformToken,"platform-console")).isNotEmpty();
        assertThatThrownBy(()->sso.active(first.getAccessToken(),"learning-workbench")).isInstanceOf(BizException.class);
        var second=exchange(code());f.transaction.executeWithoutResult(s->sso.logout(platformToken,true));
        assertThatThrownBy(()->sso.active(second.getAccessToken(),"learning-workbench")).isInstanceOf(BizException.class);
    }
    @Test void refreshRotatesOpaqueTokenAndRejectsReplay() {
        var token=exchange(code());var dto=new SsoRefreshDTO();dto.setClientId("open-api");dto.setClientSecret(secret);dto.setRefreshToken(token.getRefreshToken());
        var refreshed=f.transaction.execute(s->sso.refresh(dto));
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(token.getRefreshToken());
        assertThatThrownBy(()->f.transaction.execute(s->sso.refresh(dto))).isInstanceOf(BizException.class);
        assertThat(sso.active(refreshed.getAccessToken(),"learning-workbench")).isNotEmpty();
        String hash=f.jdbc.queryForObject("SELECT refresh_token_hash FROM sys_sso_session WHERE session_id=?",String.class,token.getSessionId());
        assertThat(hash).isNotEqualTo(refreshed.getRefreshToken()).isEqualTo(SsoCrypto.hash(refreshed.getRefreshToken()));
    }
    @Test void suspendedInstanceRejectsIssuedCodeAndActiveToken() {
        var app=exchange(code());var pending=code();
        f.jdbc.update("UPDATE sys_tenant_application SET status='SUSPENDED' WHERE id=?",instance);
        assertThatThrownBy(()->sso.active(app.getAccessToken(),"learning-workbench")).isInstanceOf(BizException.class);
        assertThatThrownBy(()->exchange(pending)).isInstanceOf(BizException.class);
    }
}
