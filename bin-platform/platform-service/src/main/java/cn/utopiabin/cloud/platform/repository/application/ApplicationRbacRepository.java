package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.mapper.application.ApplicationPersistenceMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationResourceDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationRoleDTO;
import cn.utopiabin.cloud.platform.model.dto.application.UserGrantDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationResourceVO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationRoleVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 应用成员、角色、权限与菜单数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationRbacRepository extends ApplicationRepositorySupport {
    private final ApplicationPersistenceMapper mapper;

    public List<UserApplicationVO> listMembers(long instanceId, long tenantId) {
        return convert(
                mapper.selectInstanceMembers(parameters(instanceId, instanceId, tenantId)),
                UserApplicationVO.class);
    }

    public List<Long> listMemberRoleIds(long tenantId, long instanceId, long userId) {
        return scalars(
                mapper.selectMemberRoleIds(parameters(tenantId, instanceId, userId)), Long.class);
    }

    public void lockInstance(long instanceId, long tenantId) {
        one(mapper.selectRbacInstanceForUpdate(parameters(instanceId, tenantId)));
    }

    public void requireUser(long userId, long tenantId) {
        one(mapper.selectTenantUser(parameters(userId, tenantId)));
    }

    public void requireRole(long roleId, long tenantId, long applicationId, long instanceId) {
        one(
                mapper.selectAvailableInstanceRole(
                        parameters(roleId, tenantId, applicationId, instanceId)));
    }

    public List<Map<String, Object>> lockMember(long tenantId, long instanceId, long userId) {
        return maps(mapper.selectMemberForUpdate(parameters(tenantId, instanceId, userId)));
    }

    public void insertMember(long tenantId, long instanceId, UserGrantDTO dto, long operatorId) {
        mapper.insertApplicationMember(
                parameters(
                        IdWorker.getId(),
                        tenantId,
                        instanceId,
                        dto.getUserId(),
                        dto.getStatus(),
                        dto.getEffectiveAt(),
                        dto.getExpireAt(),
                        operatorId,
                        dto.getComment()));
    }

    public int updateMember(UserGrantDTO dto, long operatorId, Object id, int version) {
        return mapper.updateApplicationMember(
                parameters(
                        dto.getStatus(),
                        dto.getEffectiveAt(),
                        dto.getExpireAt(),
                        dto.getComment(),
                        operatorId,
                        id,
                        version));
    }

    public void replaceMemberRoles(
            long tenantId, long applicationId, long instanceId, long userId, List<Long> roleIds) {
        mapper.deleteMemberRoles(parameters(tenantId, instanceId, userId));
        for (Long roleId : roleIds) {
            mapper.insertMemberRole(
                    parameters(
                            IdWorker.getId(), tenantId, applicationId, instanceId, userId, roleId));
        }
    }

    public List<ApplicationRoleVO> listRoles(long tenantId, long instanceId) {
        return convert(
                mapper.selectInstanceRoles(parameters(tenantId, instanceId)),
                ApplicationRoleVO.class);
    }

    public List<Long> listRolePermissionIds(long tenantId, long instanceId, long roleId) {
        return scalars(
                mapper.selectRolePermissionIds(parameters(tenantId, instanceId, roleId)),
                Long.class);
    }

    public void lockRoleInstance(long instanceId, long tenantId) {
        one(mapper.selectRoleInstanceForUpdate(parameters(instanceId, tenantId)));
    }

    public void requirePermission(long permissionId, long applicationId) {
        one(mapper.selectAvailableApplicationPermission(parameters(permissionId, applicationId)));
    }

    public Map<String, Object> getRole(
            long roleId, long tenantId, long applicationId, long instanceId) {
        return one(
                mapper.selectInstanceRole(parameters(roleId, tenantId, applicationId, instanceId)));
    }

    public void insertRole(
            long roleId,
            long tenantId,
            long applicationId,
            long instanceId,
            ApplicationRoleDTO dto) {
        mapper.insertApplicationRole(
                parameters(
                        roleId,
                        tenantId,
                        applicationId,
                        instanceId,
                        dto.getName(),
                        dto.getCode(),
                        dto.getDataScope(),
                        dto.isAvailable(),
                        dto.getSort()));
    }

    public int updateRole(
            long roleId, long tenantId, long instanceId, ApplicationRoleDTO dto, int version) {
        return mapper.updateApplicationRole(
                parameters(
                        dto.getName(),
                        dto.getCode(),
                        dto.getDataScope(),
                        dto.isAvailable(),
                        dto.getSort(),
                        roleId,
                        tenantId,
                        instanceId,
                        version));
    }

    public void replaceRolePermissions(
            long tenantId,
            long applicationId,
            long instanceId,
            long roleId,
            List<Long> permissionIds) {
        mapper.deleteRolePermissions(parameters(tenantId, instanceId, roleId));
        for (Long permissionId : permissionIds) {
            mapper.insertRolePermission(
                    parameters(
                            IdWorker.getId(),
                            tenantId,
                            applicationId,
                            instanceId,
                            roleId,
                            permissionId));
        }
    }

    public Map<String, Object> lockRoleForDelete(long roleId, long tenantId, long instanceId) {
        one(mapper.selectDeleteRoleInstanceForUpdate(parameters(instanceId, tenantId)));
        return one(mapper.selectRoleForDelete(parameters(roleId, tenantId, instanceId)));
    }

    public int removeRole(long roleId, long tenantId, long instanceId, int version) {
        int affected =
                mapper.softDeleteApplicationRole(parameters(roleId, tenantId, instanceId, version));
        mapper.deleteRoleMembers(parameters(tenantId, instanceId, roleId));
        mapper.deleteDeletedRolePermissions(parameters(tenantId, instanceId, roleId));
        return affected;
    }

    public List<ApplicationResourceVO> listPermissions(long applicationId) {
        return convert(
                mapper.applicationPermissions(parameters(applicationId)),
                ApplicationResourceVO.class);
    }

    public List<ApplicationResourceVO> listMenus(long applicationId) {
        return convert(
                mapper.applicationMenus(parameters(applicationId)), ApplicationResourceVO.class);
    }

    public void lockApplication(long applicationId) {
        one(mapper.selectResourceApplicationForUpdate(parameters(applicationId)));
    }

    public void lockApplicationForDelete(long applicationId) {
        one(mapper.selectDeleteResourceApplicationForUpdate(parameters(applicationId)));
    }

    public Map<String, Object> getPermission(long id, long applicationId) {
        return one(mapper.applicationPermission(parameters(id, applicationId)));
    }

    public Map<String, Object> getMenu(long id, long applicationId) {
        return one(mapper.applicationMenu(parameters(id, applicationId)));
    }

    public void insertPermission(long id, ApplicationResourceDTO dto) {
        mapper.insertApplicationPermission(
                parameters(
                        id,
                        dto.getApplicationId(),
                        dto.getName(),
                        dto.getCode(),
                        dto.getDescription(),
                        dto.isAvailable(),
                        dto.getSort()));
    }

    public int updatePermission(long id, ApplicationResourceDTO dto, int version) {
        return mapper.updateApplicationPermission(
                parameters(
                        dto.getName(),
                        dto.getDescription(),
                        dto.isAvailable(),
                        dto.getSort(),
                        id,
                        dto.getApplicationId(),
                        version));
    }

    public Map<String, Object> getMenuParent(long id, long applicationId) {
        return one(mapper.selectMenuParent(parameters(id, applicationId)));
    }

    public void requirePermissionCode(String code, long applicationId) {
        one(mapper.selectPermissionByCode(parameters(code, applicationId)));
    }

    public boolean menuHasChildren(long id, long applicationId) {
        return !mapper.selectMenuChildrenByParent(parameters(id, applicationId)).isEmpty();
    }

    public void insertMenu(long id, long parentId, ApplicationResourceDTO dto) {
        mapper.insertApplicationMenu(
                parameters(
                        id,
                        dto.getApplicationId(),
                        parentId,
                        dto.getType(),
                        dto.getName(),
                        dto.getPath(),
                        dto.getComponent(),
                        dto.getIcon(),
                        dto.getPermission(),
                        dto.getRouteName(),
                        dto.getOpenMode(),
                        dto.isVisible(),
                        dto.isAvailable(),
                        dto.getSort()));
    }

    public int updateMenu(long id, long parentId, ApplicationResourceDTO dto, int version) {
        return mapper.updateApplicationMenu(
                parameters(
                        parentId,
                        dto.getType(),
                        dto.getName(),
                        dto.getPath(),
                        dto.getComponent(),
                        dto.getIcon(),
                        dto.getPermission(),
                        dto.getRouteName(),
                        dto.getOpenMode(),
                        dto.isVisible(),
                        dto.isAvailable(),
                        dto.getSort(),
                        id,
                        dto.getApplicationId(),
                        version));
    }

    public boolean hasChildMenu(long applicationId, long menuId) {
        return !mapper.selectMenuChildren(parameters(applicationId, menuId)).isEmpty();
    }

    public boolean permissionIsReferenced(long applicationId, long permissionId, String code) {
        return !mapper.selectRolePermissionReferences(parameters(applicationId, permissionId))
                        .isEmpty()
                || !mapper.selectMenuPermissionReferences(parameters(applicationId, code))
                        .isEmpty();
    }

    public int removePermission(long id, long applicationId, int version) {
        return mapper.deleteApplicationPermission(parameters(id, applicationId, version));
    }

    public int removeMenu(long id, long applicationId, int version) {
        return mapper.deleteApplicationMenu(parameters(id, applicationId, version));
    }
}
