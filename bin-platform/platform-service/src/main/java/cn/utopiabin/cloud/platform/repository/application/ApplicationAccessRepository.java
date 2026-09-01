package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 应用访问边界数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationAccessRepository extends ApplicationRepositorySupport {
    private final ApplicationPersistenceMapper mapper;

    public Map<String, Object> getInstance(long instanceId) {
        return one(mapper.selectTenantApplicationById(parameters(instanceId)));
    }

    public Map<String, Object> getUserAccess(long userId, long instanceId, long tenantId) {
        return one(mapper.selectUserApplicationAccess(parameters(userId, instanceId, tenantId)));
    }

    public List<Map<String, Object>> listUserGrants(long tenantId, long instanceId, long userId) {
        return maps(mapper.selectUserApplicationGrant(parameters(tenantId, instanceId, userId)));
    }
}
