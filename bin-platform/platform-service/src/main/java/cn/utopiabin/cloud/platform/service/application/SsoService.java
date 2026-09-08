package cn.utopiabin.cloud.platform.service.application;

import static cn.utopiabin.cloud.platform.util.ApplicationDomainUtils.requireSingleChange;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.JsonUtil;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import cn.utopiabin.cloud.platform.entity.application.SysApplication;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.model.dto.application.SsoAuthorizeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoExchangeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoRefreshDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationProfileVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuthorizeVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;
import cn.utopiabin.cloud.platform.repository.application.ApplicationCatalogRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationMenuRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationPermissionRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationRoleRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationSessionRepository;
import cn.utopiabin.cloud.platform.repository.application.ApplicationUserRepository;
import cn.utopiabin.cloud.platform.repository.application.model.ApplicationAccessRecord;
import cn.utopiabin.cloud.platform.service.application.model.ActiveApplicationSession;
import cn.utopiabin.cloud.platform.util.JwtTokenService;
import cn.utopiabin.cloud.platform.util.MenuTreeBuilder;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SsoService {
  private final ApplicationSessionRepository repository;
  private final ApplicationCatalogRepository catalog;
  private final ApplicationUserRepository users;
  private final ApplicationRoleRepository roleRepository;
  private final ApplicationPermissionRepository permissionRepository;
  private final ApplicationMenuRepository menuRepository;
  private final ApplicationBoundary boundary;
  private final SsoTicketStore tickets;
  private final JwtTokenService jwt;
  private final JwtTokenProperties jwtProperties;
  private final SsoAuditService audit;

  public record Ticket(
      long tenant,
      long user,
      long instance,
      long app,
      int appVersion,
      int instanceVersion,
      String parent,
      String redirect,
      String challenge) {}

  @Transactional
  public String platformLogin(SysUser user, List<String> roles) {
    var access = boundary.access(user.getTenantId(), user.getId(), user.getTenantId());
    var result = create(access, null, roles, Math.max(60, jwtProperties.getJwtExpiration()));
    users.updateLastLogin(user.getId(), user.getTenantId());
    return result.getAccessToken();
  }

  public ActiveApplicationSession active(String token, String audience) {
    var claims = jwt.claims(token);
    if (claims.getAudience() == null || !claims.getAudience().equals(Set.of(audience)))
      throw new BizException(401, "Token不属于目标应用");
    String sid = claims.get("sid", String.class);
    if (sid == null || sid.isBlank()) throw new BizException(401, "旧登录态不再支持，请重新登录");
    var session = repository.getSession(sid);
    if (!"ACTIVE".equals(session.getStatus())
        || !session.getExpireAt().isAfter(LocalDateTime.now()))
      throw new BizException(401, "会话已撤销或过期");
    if (!String.valueOf(session.getUserId()).equals(claims.get("userId", String.class))
        || !String.valueOf(session.getTenantId()).equals(claims.get("tenantId", String.class))
        || !String.valueOf(session.getTenantApplicationId())
            .equals(claims.get("tenantApplicationId", String.class)))
      throw new BizException(401, "Token会话边界不匹配");
    var access =
        boundary.access(
            session.getTenantId(), session.getUserId(), session.getTenantApplicationId());
    Number credential = claims.get("credentialVersion", Number.class);
    if (!audience.equals(access.getApplicationCode())
        || session.getApplicationId().longValue() != access.getApplicationId().longValue()
        || credential == null
        || credential.intValue() != access.getCredentialVersion()
        || session.getCredentialVersion().intValue() != access.getCredentialVersion().intValue())
      throw new BizException(401, "密码或应用身份已变化，请重新登录");
    String parent = session.getParentSessionId();
    if (parent != null && !parent.isBlank()) {
      if (!repository.parentSessionActive(parent, session.getTenantId(), session.getUserId())) {
        throw new BizException(401, "平台登录已退出，请重新登录");
      }
    }

    // access.id 表示租户应用实例，返回会话标识时必须使用 session_id，不能误用会话表数字主键。
    return new ActiveApplicationSession(session, access);
  }

  public SsoAuthorizeVO authorize(String token, SsoAuthorizeDTO dto) {
    ActiveApplicationSession parent = null;
    try {
      parent = active(token, "platform-console");
      var access =
          boundary.access(
              parent.session().getTenantId(),
              parent.session().getUserId(),
              dto.getTenantApplicationId());
      long app = access.getApplicationId();
      var product = catalog.getApplication(app);
      if (app == 1
          || !Boolean.TRUE.equals(product.getSsoEnabled())
          || product.getClientSecretHash() == null)
        throw new BizException(400, "目标应用未启用SSO或尚未配置后端客户端凭证");
      whitelist(app, dto.getRedirectUri());
      String code = SsoCrypto.random();
      var ticket =
          new Ticket(
              parent.session().getTenantId(),
              parent.session().getUserId(),
              dto.getTenantApplicationId(),
              app,
              (int) product.getVersion(),
              access.getVersion(),
              parent.session().getSessionId(),
              dto.getRedirectUri(),
              dto.getCodeChallenge());
      tickets.put(code, JsonUtil.toJson(ticket));
      audit.record(
          "TICKET_ISSUED",
          true,
          "",
          ticket.tenant(),
          app,
          ticket.instance(),
          ticket.user(),
          ticket.parent());
      var result = new SsoAuthorizeVO();
      result.setExpiresIn(60);
      result.setRedirectUrl(
          dto.getRedirectUri()
              + "?code="
              + code
              + "&state="
              + URLEncoder.encode(dto.getState(), StandardCharsets.UTF_8));
      return result;
    } catch (BizException e) {
      audit.record(
          "TICKET_ISSUED",
          false,
          "AUTHORIZE_REJECTED",
          parent == null ? null : parent.session().getTenantId(),
          null,
          dto.getTenantApplicationId(),
          parent == null ? null : parent.session().getUserId(),
          null);
      throw e;
    }
  }

  private SysApplication client(String clientId, String secret) {
    var client = catalog.lockClient(clientId);
    if (client == null
        || secret == null
        || !SsoCrypto.equal(client.getClientSecretHash(), SsoCrypto.hash(secret)))
      throw new BizException(401, "应用后端客户端凭证无效");
    return client;
  }

  private void whitelist(long app, String uri) {
    if (catalog.listRedirectUris(app).stream().noneMatch(uri::equals))
      throw new BizException(400, "回调地址不在当前应用的精确白名单内");
  }

  @Transactional
  public SsoTokenVO exchange(SsoExchangeDTO dto) {
    Ticket ticket = null;
    try {
      var app = client(dto.getClientId(), dto.getClientSecret());
      String encoded = tickets.consume(dto.getCode());
      ticket = encoded == null ? null : JsonUtil.toObject(encoded, Ticket.class);
      if (ticket == null) throw new BizException(400, "授权码无效、过期或已经使用，请重新发起登录");
      if (ticket.app() != app.getId()
          || ticket.appVersion() != app.getVersion()
          || !ticket.redirect().equals(dto.getRedirectUri())
          || !SsoCrypto.equal(ticket.challenge(), SsoCrypto.challenge(dto.getCodeVerifier())))
        throw new BizException(400, "授权码的应用、回调地址或PKCE校验失败");
      whitelist(ticket.app(), ticket.redirect());
      var parent = repository.getPlatformSession(ticket.parent(), ticket.tenant(), ticket.user());
      var access = boundary.access(ticket.tenant(), ticket.user(), ticket.instance());
      if (ticket.instanceVersion() != access.getVersion()
          || parent.getCredentialVersion().intValue() != access.getCredentialVersion().intValue())
        throw new BizException(401, "授权期间身份或开通状态已变化，请重新登录");
      long ttl =
          Math.min(28800, Duration.between(LocalDateTime.now(), parent.getExpireAt()).getSeconds());
      var result =
          create(
              access,
              ticket.parent(),
              roles(ticket.tenant(), ticket.user(), ticket.instance()),
              ttl);
      return result;
    } catch (BizException e) {
      audit.record(
          "CODE_EXCHANGED",
          false,
          "EXCHANGE_REJECTED",
          ticket == null ? null : ticket.tenant(),
          ticket == null ? null : ticket.app(),
          ticket == null ? null : ticket.instance(),
          ticket == null ? null : ticket.user(),
          null);
      throw e;
    }
  }

  private SsoTokenVO create(
      ApplicationAccessRecord access, String parent, List<String> roles, long lifetime) {
    for (LocalDateTime end :
        new LocalDateTime[] {
          access.getExpireAt(), access.getTenantExpire(), access.getGrantExpire()
        }) {
      if (end != null)
        lifetime = Math.min(lifetime, Duration.between(LocalDateTime.now(), end).getSeconds());
    }
    if (lifetime < 1) throw new BizException(401, "平台会话已过期");
    long tenant = access.getTenantId(), app = access.getApplicationId(), instance = access.getId();
    long user = access.getUserId();
    String sid = SsoCrypto.random(), refresh = parent == null ? null : SsoCrypto.random();
    repository.insertSession(
        sid,
        parent,
        tenant,
        app,
        instance,
        user,
        access.getCredentialVersion(),
        refresh == null ? null : SsoCrypto.hash(refresh),
        LocalDateTime.now().plusSeconds(lifetime));
    var result =
        token(access, sid, roles, refresh, parent == null ? lifetime : Math.min(300, lifetime));
    TransactionAfterCommitExecutor.afterCommit(
        () ->
            audit.record(
                parent == null ? "PLATFORM_LOGIN" : "CODE_EXCHANGED",
                true,
                "",
                tenant,
                app,
                instance,
                user,
                sid));
    return result;
  }

  private SsoTokenVO token(
      ApplicationAccessRecord access, String sid, List<String> roles, String refresh, long ttl) {
    var result = new SsoTokenVO();
    result.setSessionId(sid);
    result.setApplicationCode(access.getApplicationCode());
    result.setExpiresIn(ttl);
    result.setRefreshToken(refresh);
    result.setAccessToken(
        jwt.generateScoped(
            String.valueOf(access.getUserId()),
            access.getUsername(),
            String.valueOf(access.getTenantId()),
            roles,
            result.getApplicationCode(),
            String.valueOf(access.getId()),
            sid,
            access.getCredentialVersion(),
            ttl));
    return result;
  }

  @Transactional
  public SsoTokenVO refresh(SsoRefreshDTO dto) {
    try {
      var app = client(dto.getClientId(), dto.getClientSecret());
      var session =
          repository.getRefreshSession(SsoCrypto.hash(dto.getRefreshToken()), app.getId());
      var access =
          boundary.access(
              session.getTenantId(), session.getUserId(), session.getTenantApplicationId());
      if (session.getCredentialVersion().intValue() != access.getCredentialVersion().intValue())
        throw new BizException(401, "密码已变更");
      repository.requirePlatformSession(
          session.getParentSessionId(), session.getTenantId(), session.getUserId());
      String refresh = SsoCrypto.random(), sid = session.getSessionId();
      requireSingleChange(
          repository.rotateRefreshToken(
              SsoCrypto.hash(refresh), sid, SsoCrypto.hash(dto.getRefreshToken())));
      long ttl =
          Math.min(300, Duration.between(LocalDateTime.now(), session.getExpireAt()).getSeconds());
      if (ttl < 1) throw new BizException(401, "会话已过期");
      TransactionAfterCommitExecutor.afterCommit(
          () ->
              audit.record(
                  "TOKEN_REFRESHED",
                  true,
                  "",
                  session.getTenantId(),
                  app.getId(),
                  session.getTenantApplicationId(),
                  session.getUserId(),
                  sid));
      return token(
          access,
          sid,
          roles(session.getTenantId(), session.getUserId(), session.getTenantApplicationId()),
          refresh,
          ttl);
    } catch (BizException e) {
      audit.record("TOKEN_REFRESHED", false, "REFRESH_REJECTED", null, null, null, null, null);
      throw e;
    }
  }

  public List<String> roles(long tenant, long user, long instance) {
    return roleRepository.listRoleCodes(tenant, user, instance);
  }

  public ApplicationProfileVO profile(String token, String audience) {
    var access = active(token, audience);
    long tenant = access.session().getTenantId(),
        user = access.session().getUserId(),
        instance = access.session().getTenantApplicationId(),
        app = access.session().getApplicationId();
    var codes = permissionRepository.listPermissionCodes(app, tenant, instance, user);
    var all = menuRepository.listAvailableMenus(app);
    var menus =
        all.stream()
            .filter(
                m ->
                    m.getPermission() == null
                        || m.getPermission().isBlank()
                        || codes.contains(m.getPermission())
                        || codes.contains("*"))
            .toList();
    var result = new ApplicationProfileVO();
    result.setTenantId(tenant);
    result.setUserId(user);
    result.setUsername(access.identity().getUsername());
    result.setApplicationId(app);
    result.setTenantApplicationId(instance);
    result.setApplicationCode(audience);
    result.setApplicationName(access.identity().getApplicationName());
    result.setSessionId(access.session().getSessionId());
    result.setRoles(roles(tenant, user, instance));
    result.setPermissionCodes(codes);
    result.setMenus(MenuTreeBuilder.build(menus));
    return result;
  }

  @Transactional
  public void logout(String token, boolean global) {
    var claims = jwt.claims(token);
    String sid = claims.get("sid", String.class);
    if (sid == null) return;
    if (global
        && (claims.getAudience() == null
            || !claims.getAudience().equals(Set.of("platform-console"))))
      throw new BizException(403, "应用会话不能注销平台会话");
    if (global) {
      repository.logoutGlobally(sid);
    } else {
      repository.logout(sid);
    }
    TransactionAfterCommitExecutor.afterCommit(
        () ->
            audit.record(
                "LOGOUT",
                true,
                "",
                Long.valueOf(claims.get("tenantId", String.class)),
                null,
                Long.valueOf(claims.get("tenantApplicationId", String.class)),
                Long.valueOf(claims.get("userId", String.class)),
                sid));
  }
}
