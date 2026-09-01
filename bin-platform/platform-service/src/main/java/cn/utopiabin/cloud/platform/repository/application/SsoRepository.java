package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.iam.SysMenu;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 单点登录认证数据仓库。 */
@Repository
@RequiredArgsConstructor
public class SsoRepository extends ApplicationRepositorySupport {
    private final ApplicationPersistenceMapper mapper;

    public void updateLastLogin(long userId, long tenantId) {
        mapper.updateUserLastLogin(parameters(userId, tenantId));
    }

    public Map<String, Object> getSession(String sessionId) {
        return one(mapper.selectSsoSession(parameters(sessionId)));
    }

    public boolean parentSessionActive(String sessionId, Object tenantId, Object userId) {
        return !mapper.selectActiveParentSession(parameters(sessionId, tenantId, userId)).isEmpty();
    }

    public Map<String, Object> getApplication(long applicationId) {
        return one(mapper.selectSsoApplication(parameters(applicationId)));
    }

    public List<Map<String, Object>> lockClient(String clientId) {
        return maps(mapper.selectSsoClientForUpdate(parameters(clientId)));
    }

    public List<String> listRedirectUris(long applicationId) {
        return scalars(
                mapper.selectApplicationRedirectUris(parameters(applicationId)), String.class);
    }

    public Map<String, Object> getPlatformSession(String sessionId, long tenantId, long userId) {
        return one(mapper.selectActivePlatformSession(parameters(sessionId, tenantId, userId)));
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
        mapper.insertSsoSession(
                parameters(
                        IdWorker.getId(),
                        sessionId,
                        parentSessionId,
                        tenantId,
                        applicationId,
                        instanceId,
                        userId,
                        credentialVersion,
                        refreshTokenHash,
                        expireAt));
    }

    public Map<String, Object> getRefreshSession(String refreshTokenHash, long applicationId) {
        return one(mapper.selectSessionByRefreshToken(parameters(refreshTokenHash, applicationId)));
    }

    public void requirePlatformSession(Object sessionId, Object tenantId, Object userId) {
        one(mapper.selectRefreshParentSession(parameters(sessionId, tenantId, userId)));
    }

    public int rotateRefreshToken(
            String refreshTokenHash, String sessionId, String previousRefreshTokenHash) {
        return mapper.rotateSsoRefreshToken(
                parameters(refreshTokenHash, sessionId, previousRefreshTokenHash));
    }

    public List<String> listRoleCodes(long tenantId, long userId, long instanceId) {
        return scalars(
                mapper.selectUserApplicationRoleCodes(parameters(tenantId, userId, instanceId)),
                String.class);
    }

    public List<String> listPermissionCodes(
            long applicationId, long tenantId, long instanceId, long userId) {
        return scalars(
                mapper.selectUserApplicationPermissionCodes(
                        parameters(applicationId, tenantId, instanceId, userId)),
                String.class);
    }

    public List<SysMenu> listMenus(long applicationId) {
        return convert(
                mapper.selectApplicationMenusForProfile(parameters(applicationId)), SysMenu.class);
    }

    public void logout(String sessionId) {
        mapper.logoutSession(parameters(sessionId));
    }

    public void logoutGlobally(String sessionId) {
        mapper.logoutGlobally(parameters(sessionId));
    }
}
