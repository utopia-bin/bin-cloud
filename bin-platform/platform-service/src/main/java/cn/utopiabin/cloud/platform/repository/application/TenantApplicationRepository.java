package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.InstanceDTO;
import cn.utopiabin.cloud.platform.model.vo.application.TenantApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 租户应用实例数据仓库。 */
@Repository
@RequiredArgsConstructor
public class TenantApplicationRepository extends ApplicationRepositorySupport {
    private final ApplicationPersistenceMapper mapper;

    public PageResult<TenantApplicationVO> page(ApplicationQuery query, Long tenantId) {
        Map<String, Object> parameters = pageParameters(query, tenantId);
        Long total = scalarLong(mapper.tenantApplicationCount(parameters));
        return page(
                query, total, mapper.tenantApplicationPage(parameters), TenantApplicationVO.class);
    }

    public List<TenantApplicationVO> listAvailable(long tenantId) {
        return convert(
                mapper.tenantApplicationMine(parameters(tenantId)), TenantApplicationVO.class);
    }

    public boolean consoleExists(long tenantId) {
        Long count = scalarLong(mapper.consoleInstanceCount(parameters(tenantId)));
        return count != null && count > 0;
    }

    public void insertConsole(long tenantId) {
        mapper.insertConsoleInstance(parameters(tenantId));
    }

    public Map<String, Object> lockApplication(long applicationId) {
        return one(mapper.selectProvisionApplicationForUpdate(parameters(applicationId)));
    }

    public Map<String, Object> lockTenant(long tenantId) {
        return one(mapper.selectProvisionTenantForUpdate(parameters(tenantId)));
    }

    public Map<String, Object> lockInstance(long instanceId, long tenantId, long applicationId) {
        return one(
                mapper.selectTenantApplicationForUpdate(
                        parameters(instanceId, tenantId, applicationId)));
    }

    public void requireAvailableUser(long userId, long tenantId) {
        one(mapper.selectAvailableTenantUser(parameters(userId, tenantId)));
    }

    public List<UserApplicationVO> listCandidateUsers(long tenantId) {
        return convert(
                mapper.selectTenantApplicationCandidates(parameters(tenantId)),
                UserApplicationVO.class);
    }

    public boolean instanceExists(long tenantId, long applicationId) {
        return !mapper.selectTenantApplicationId(parameters(tenantId, applicationId)).isEmpty();
    }

    public List<Map<String, Object>> listApplicationPermissions(long applicationId) {
        return maps(mapper.selectProvisionApplicationPermissions(parameters(applicationId)));
    }

    public int updateInstance(InstanceDTO dto, long operatorId, int expectedVersion) {
        return mapper.updateTenantApplication(
                parameters(
                        dto.getStatus(),
                        dto.getAccessPolicy(),
                        dto.getEntryUrlOverride(),
                        dto.getEffectiveAt(),
                        dto.getExpireAt(),
                        dto.getComment(),
                        dto.getStatus(),
                        dto.getStatus(),
                        String.valueOf(operatorId),
                        dto.getId(),
                        dto.getTenantId(),
                        dto.getApplicationId(),
                        expectedVersion));
    }

    public void insertInstance(long instanceId, InstanceDTO dto, long operatorId) {
        mapper.insertTenantApplication(
                parameters(
                        instanceId,
                        dto.getTenantId(),
                        dto.getApplicationId(),
                        dto.getAccessPolicy(),
                        dto.getEntryUrlOverride(),
                        dto.getEffectiveAt(),
                        dto.getExpireAt(),
                        dto.getComment(),
                        String.valueOf(operatorId),
                        String.valueOf(operatorId)));
    }

    public void insertAdministratorRole(
            long roleId, long tenantId, long applicationId, long instanceId) {
        mapper.insertTenantApplicationAdminRole(
                parameters(roleId, tenantId, applicationId, instanceId));
    }

    public void grantRolePermission(
            long tenantId, long applicationId, long instanceId, long roleId, Object permissionId) {
        mapper.insertTenantApplicationRolePermission(
                parameters(
                        IdWorker.getId(),
                        tenantId,
                        applicationId,
                        instanceId,
                        roleId,
                        permissionId));
    }

    public void assignRole(
            long tenantId, long applicationId, long instanceId, long userId, long roleId) {
        mapper.insertTenantApplicationUserRole(
                parameters(IdWorker.getId(), tenantId, applicationId, instanceId, userId, roleId));
    }

    public void grantApplication(long tenantId, long instanceId, long userId, long operatorId) {
        mapper.insertTenantApplicationGrant(
                parameters(IdWorker.getId(), tenantId, instanceId, userId, operatorId));
    }

    public void activate(long instanceId) {
        mapper.activateTenantApplication(parameters(instanceId));
    }
}
