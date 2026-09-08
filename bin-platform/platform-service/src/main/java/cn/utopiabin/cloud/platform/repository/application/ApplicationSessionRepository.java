package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.entity.application.SysSsoSession;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationSessionMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.vo.application.SsoSessionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** SSO会话生命周期数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationSessionRepository extends ApplicationRepositorySupport {

  private final ApplicationSessionMapper mapper;

  public PageResult<SsoSessionVO> pageSessions(ApplicationQuery query, Long tenantId) {
    int page = Math.max(1, query.getPage());
    int size = Math.max(1, Math.min(100, query.getSize()));
    return PageResult.of(
        page,
        size,
        mapper.sessionCount(query, tenantId),
        mapper.sessionPage(query, tenantId, (long) (page - 1) * size, size));
  }

  public SysSsoSession lockSession(String sessionId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysSsoSession>()
                .eq(SysSsoSession::getSessionId, sessionId)
                .last("FOR UPDATE")));
  }

  public SysSsoSession getSession(String sessionId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysSsoSession>().eq(SysSsoSession::getSessionId, sessionId)));
  }

  private LambdaQueryWrapper<SysSsoSession> activePlatformSession(
      String sessionId, long tenantId, long userId) {
    return new LambdaQueryWrapper<SysSsoSession>()
        .eq(SysSsoSession::getSessionId, sessionId)
        .eq(SysSsoSession::getApplicationId, 1L)
        .eq(SysSsoSession::getTenantId, tenantId)
        .eq(SysSsoSession::getUserId, userId)
        .eq(SysSsoSession::getStatus, "ACTIVE")
        .apply("expire_at > CURRENT_TIMESTAMP");
  }

  public boolean parentSessionActive(String sessionId, long tenantId, long userId) {
    return mapper.selectCount(activePlatformSession(sessionId, tenantId, userId)) > 0;
  }

  public SysSsoSession getPlatformSession(String sessionId, long tenantId, long userId) {
    return require(mapper.selectOne(activePlatformSession(sessionId, tenantId, userId)));
  }

  public void requirePlatformSession(String sessionId, long tenantId, long userId) {
    // 刷新应用令牌必须依附有效的平台会话，不能用其他应用会话充当父会话。
    getPlatformSession(sessionId, tenantId, userId);
  }

  public void insertSession(
      String sessionId,
      String parentSessionId,
      long tenantId,
      long applicationId,
      long instanceId,
      long userId,
      long credentialVersion,
      String refreshTokenHash,
      LocalDateTime expireAt) {
    SysSsoSession session = new SysSsoSession();
    session.setId(IdWorker.getId());
    session.setSessionId(sessionId);
    session.setParentSessionId(parentSessionId);
    session.setTenantId(tenantId);
    session.setApplicationId(applicationId);
    session.setTenantApplicationId(instanceId);
    session.setUserId(userId);
    session.setCredentialVersion(Math.toIntExact(credentialVersion));
    session.setRefreshTokenHash(refreshTokenHash);
    session.setExpireAt(expireAt);
    mapper.insert(session);
  }

  public SysSsoSession getRefreshSession(String refreshTokenHash, long applicationId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysSsoSession>()
                .eq(SysSsoSession::getRefreshTokenHash, refreshTokenHash)
                .eq(SysSsoSession::getApplicationId, applicationId)
                .eq(SysSsoSession::getStatus, "ACTIVE")
                .apply("expire_at > CURRENT_TIMESTAMP")
                .last("FOR UPDATE")));
  }

  public int rotateRefreshToken(
      String refreshTokenHash, String sessionId, String previousRefreshTokenHash) {
    // 同时匹配旧哈希，避免并发刷新重复消费同一令牌。
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysSsoSession>()
            .set(SysSsoSession::getRefreshTokenHash, refreshTokenHash)
            .setSql("last_access_at = CURRENT_TIMESTAMP, version = version + 1")
            .eq(SysSsoSession::getSessionId, sessionId)
            .eq(SysSsoSession::getRefreshTokenHash, previousRefreshTokenHash)
            .eq(SysSsoSession::getStatus, "ACTIVE")
            .apply("expire_at > CURRENT_TIMESTAMP"));
  }

  private LambdaUpdateWrapper<SysSsoSession> revocation(String reason) {
    // 只撤销仍有效的记录，重复下线不能覆盖首次撤销原因和时间。
    return new LambdaUpdateWrapper<SysSsoSession>()
        .set(SysSsoSession::getStatus, "REVOKED")
        .set(SysSsoSession::getRevokeReason, reason)
        .setSql("revoked_at = CURRENT_TIMESTAMP, version = version + 1")
        .eq(SysSsoSession::getStatus, "ACTIVE");
  }

  public int revokeSession(long tenantId, String sessionId) {
    return mapper.update(
        null,
        revocation("ADMIN_REVOKED")
            .eq(SysSsoSession::getTenantId, tenantId)
            .and(
                scope ->
                    scope
                        .eq(SysSsoSession::getSessionId, sessionId)
                        .or()
                        .eq(SysSsoSession::getParentSessionId, sessionId)));
  }

  public void logout(String sessionId) {
    mapper.update(null, revocation("LOGOUT").eq(SysSsoSession::getSessionId, sessionId));
  }

  public void logoutGlobally(String sessionId) {
    mapper.update(
        null,
        revocation("GLOBAL_LOGOUT")
            .and(
                scope ->
                    scope
                        .eq(SysSsoSession::getSessionId, sessionId)
                        .or()
                        .eq(SysSsoSession::getParentSessionId, sessionId)));
  }

  public int revokeByApplication(String reason, long applicationId) {
    return mapper.update(
        null, revocation(reason).eq(SysSsoSession::getApplicationId, applicationId));
  }

  public int revokeByInstance(String reason, long tenantId, long instanceId) {
    return mapper.update(
        null,
        revocation(reason)
            .eq(SysSsoSession::getTenantId, tenantId)
            .eq(SysSsoSession::getTenantApplicationId, instanceId));
  }

  public int revokeByUser(String reason, long tenantId, long userId) {
    return mapper.update(
        null,
        revocation(reason)
            .eq(SysSsoSession::getTenantId, tenantId)
            .eq(SysSsoSession::getUserId, userId));
  }

  public int revokeByTenant(String reason, long tenantId) {
    return mapper.update(null, revocation(reason).eq(SysSsoSession::getTenantId, tenantId));
  }

  public int revokeByMember(String reason, long tenantId, long instanceId, long userId) {
    return mapper.update(
        null,
        revocation(reason)
            .eq(SysSsoSession::getTenantId, tenantId)
            .eq(SysSsoSession::getTenantApplicationId, instanceId)
            .eq(SysSsoSession::getUserId, userId));
  }
}
