package cn.utopiabin.cloud.platform.api.impl.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import cn.utopiabin.cloud.platform.model.vo.application.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import cn.utopiabin.cloud.platform.api.application.ApplicationApi;
import cn.utopiabin.cloud.platform.service.application.*;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.validation.annotation.Validated;

@DubboService
@Validated
@RequiredArgsConstructor
public class ApplicationApiImpl implements ApplicationApi {
    private final ApplicationCatalogService catalog;
    private final TenantApplicationService instances;
    private final ApplicationRbacService rbac;
    private final ApplicationSessionAdminService sessions;

    @Override public PageResult<ApplicationVO> applications(ApplicationQuery query) { return catalog.page(query); }
    @Override public ApplicationVO application(long id) { return catalog.get(id); }
    @Override public long saveApplication(ApplicationDTO dto) { return catalog.save(dto); }
    @Override public void removeApplication(long id, int version) { catalog.remove(id,version); }
    @Override public ClientSecretVO rotateClientSecret(long id, int version) { return catalog.rotate(id,version); }
    @Override public PageResult<TenantApplicationVO> instances(ApplicationQuery query) { return instances.page(query); }
    @Override public long saveInstance(InstanceDTO dto) { return instances.save(dto); }
    @Override public List<TenantApplicationVO> mine() { return instances.mine(); }
    @Override public List<UserApplicationVO> candidates(long tenantId) { return instances.candidates(tenantId); }
    @Override public List<UserApplicationVO> members(long instanceId) { return rbac.members(instanceId); }
    @Override public void grant(UserGrantDTO dto) { rbac.grant(dto); }
    @Override public List<ApplicationRoleVO> roles(long instanceId) { return rbac.roles(instanceId); }
    @Override public long saveRole(ApplicationRoleDTO dto) { return rbac.saveRole(dto); }
    @Override public void removeRole(long instanceId, long id, int version) { rbac.removeRole(instanceId,id,version); }
    @Override public List<ApplicationResourceVO> resources(long applicationId, String kind) { return rbac.resources(applicationId,kind); }
    @Override public long saveResource(String kind, ApplicationResourceDTO dto) { return rbac.saveResource(kind,dto); }
    @Override public void removeResource(long applicationId, String kind, long id, int version) { rbac.removeResource(applicationId,kind,id,version); }
    @Override public PageResult<SsoSessionVO> sessions(ApplicationQuery query) { return sessions.sessions(query); }
    @Override public PageResult<SsoAuditVO> audit(ApplicationQuery query) { return sessions.logs(query); }
    @Override public void revokeSession(String sessionId) { sessions.revoke(sessionId); }
}
