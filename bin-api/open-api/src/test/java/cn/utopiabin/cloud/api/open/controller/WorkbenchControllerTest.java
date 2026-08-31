package cn.utopiabin.cloud.api.open.controller;
import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.api.application.SsoApi;
import cn.utopiabin.cloud.platform.model.dto.application.SsoExchangeDTO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationProfileVO;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkbenchControllerTest {
    WorkbenchController controller;
    SsoApi sso;
    MockHttpServletRequest request;
    @BeforeEach void setup() {
        controller=new WorkbenchController();sso=mock(SsoApi.class);request=new MockHttpServletRequest();
        ReflectionTestUtils.setField(controller,"sso",sso);
        ReflectionTestUtils.setField(controller,"baseUrl","http://localhost:5173");
        ReflectionTestUtils.setField(controller,"secret","fixture-client-secret");
    }
    void login() {
        controller.start(10,request);
        String state=(String)request.getSession().getAttribute("ssoState");
        var tokens=new SsoTokenVO();tokens.setAccessToken("fixture-access");tokens.setRefreshToken("fixture-refresh");tokens.setExpiresIn(300);
        when(sso.exchange(any())).thenReturn(tokens);
        assertThat(controller.callback("fixture-code",state,request).getHeaders().getLocation().toString()).endsWith("/applications/workbench");
    }
    @Test void localHttpStartCreatesStateAndPkceWithoutPuttingCredentialsInUrl() {
        var response=controller.start(10,request);
        var url=response.getHeaders().getLocation();
        assertThat(url.toString()).startsWith("http://localhost:5173/sso/launch").doesNotContain("secret","verifier");
        var params=UriComponentsBuilder.fromUri(url).build().getQueryParams();
        assertThat(URLDecoder.decode(params.getFirst("redirectUri"),StandardCharsets.UTF_8)).isEqualTo("http://localhost:5173/api/open/workbench/callback");
        assertThat(params.getFirst("codeChallenge")).hasSize(43);
        assertThat(request.getSession().getAttribute("ssoVerifier")).isNotNull();
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }
    @Test void invalidStateNeverReachesTokenExchange() {
        controller.start(10,request);
        assertThatThrownBy(()->controller.callback("code","wrong-state",request)).isInstanceOf(BizException.class);
        verifyNoInteractions(sso);
        assertThat(request.getSession().getAttribute("ssoState")).isNull();
    }
    @Test void callbackConsumesStateAndExchangesOnlyOnBackend() {
        login();
        var captor=org.mockito.ArgumentCaptor.forClass(SsoExchangeDTO.class);
        verify(sso).exchange(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isEqualTo("fixture-client-secret");
        assertThat(captor.getValue().getCodeVerifier()).hasSize(43);
        assertThatThrownBy(()->controller.callback("fixture-code","replay",request)).isInstanceOf(BizException.class);
    }
    @Test void executeChecksCsrfAndApplicationPermission() {
        login();
        assertThatThrownBy(()->controller.execute("wrong",request)).isInstanceOf(BizException.class);
        var profile=new ApplicationProfileVO();profile.setPermissionCodes(List.of());
        when(sso.profile("fixture-access","learning-workbench")).thenReturn(profile);
        String csrf=(String)request.getSession().getAttribute("workbenchCsrf");
        assertThatThrownBy(()->controller.execute(csrf,request)).isInstanceOf(BizException.class).hasMessageContaining("权限");
        profile.setPermissionCodes(List.of("workbench:execute"));profile.setTenantApplicationId(10L);
        assertThat(controller.execute(csrf,request).getData()).contains("10");
    }
    @Test void applicationLogoutDoesNotRequestGlobalLogout() {
        login();String csrf=(String)request.getSession().getAttribute("workbenchCsrf");
        controller.logout(csrf,request);
        verify(sso).logout("fixture-access",false);
        assertThat(request.getSession(false)).isNull();
    }
}
