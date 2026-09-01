package cn.utopiabin.cloud.platform.mapper.application;

import cn.utopiabin.cloud.platform.entity.application.SysApplication;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/** 应用域持久化 Mapper，复杂查询定义在对应 XML 中。 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface ApplicationPersistenceMapper extends BaseMapper<SysApplication> {

    List<Object> selectTenantApplicationById(Map<String, Object> parameters);

    List<Object> selectUserApplicationAccess(Map<String, Object> parameters);

    List<Object> selectUserApplicationGrant(Map<String, Object> parameters);

    List<Object> selectApplicationForUpdate(Map<String, Object> parameters);

    List<Object> selectApplicationIdForUpdate(Map<String, Object> parameters);

    List<Object> selectApplicationServiceForUpdate(Map<String, Object> parameters);

    List<Object> selectApplicationDetail(Map<String, Object> parameters);

    List<Object> selectApplicationRedirects(Map<String, Object> parameters);

    List<Object> countActiveTenantApplications(Map<String, Object> parameters);

    int insertApplication(Map<String, Object> parameters);

    int updateApplication(Map<String, Object> parameters);

    int deleteApplicationRedirects(Map<String, Object> parameters);

    int insertApplicationRedirect(Map<String, Object> parameters);

    int softDeleteApplication(Map<String, Object> parameters);

    int updateApplicationClientSecret(Map<String, Object> parameters);

    List<Object> selectRbacInstanceForUpdate(Map<String, Object> parameters);

    List<Object> selectTenantUser(Map<String, Object> parameters);

    List<Object> selectAvailableInstanceRole(Map<String, Object> parameters);

    List<Object> selectRoleInstanceForUpdate(Map<String, Object> parameters);

    List<Object> selectAvailableApplicationPermission(Map<String, Object> parameters);

    List<Object> selectInstanceRole(Map<String, Object> parameters);

    List<Object> selectDeleteRoleInstanceForUpdate(Map<String, Object> parameters);

    List<Object> selectRoleForDelete(Map<String, Object> parameters);

    List<Object> selectResourceApplicationForUpdate(Map<String, Object> parameters);

    List<Object> selectMenuParent(Map<String, Object> parameters);

    List<Object> selectPermissionByCode(Map<String, Object> parameters);

    List<Object> selectDeleteResourceApplicationForUpdate(Map<String, Object> parameters);

    List<Object> selectInstanceMembers(Map<String, Object> parameters);

    List<Object> selectInstanceRoles(Map<String, Object> parameters);

    List<Object> selectMemberRoleIds(Map<String, Object> parameters);

    List<Object> selectMemberForUpdate(Map<String, Object> parameters);

    List<Object> selectRolePermissionIds(Map<String, Object> parameters);

    List<Object> selectMenuChildrenByParent(Map<String, Object> parameters);

    List<Object> selectMenuChildren(Map<String, Object> parameters);

    List<Object> selectRolePermissionReferences(Map<String, Object> parameters);

    List<Object> selectMenuPermissionReferences(Map<String, Object> parameters);

    int insertApplicationMember(Map<String, Object> parameters);

    int updateApplicationMember(Map<String, Object> parameters);

    int deleteMemberRoles(Map<String, Object> parameters);

    int insertMemberRole(Map<String, Object> parameters);

    int insertApplicationRole(Map<String, Object> parameters);

    int updateApplicationRole(Map<String, Object> parameters);

    int deleteRolePermissions(Map<String, Object> parameters);

    int insertRolePermission(Map<String, Object> parameters);

    int softDeleteApplicationRole(Map<String, Object> parameters);

    int deleteRoleMembers(Map<String, Object> parameters);

    int deleteDeletedRolePermissions(Map<String, Object> parameters);

    int insertApplicationPermission(Map<String, Object> parameters);

    int updateApplicationPermission(Map<String, Object> parameters);

    int insertApplicationMenu(Map<String, Object> parameters);

    int updateApplicationMenu(Map<String, Object> parameters);

    List<Object> selectSsoSessionForUpdate(Map<String, Object> parameters);

    int revokeSsoSessionByAdmin(Map<String, Object> parameters);

    List<Object> selectSsoSession(Map<String, Object> parameters);

    List<Object> selectSsoApplication(Map<String, Object> parameters);

    List<Object> selectActivePlatformSession(Map<String, Object> parameters);

    List<Object> selectSessionByRefreshToken(Map<String, Object> parameters);

    List<Object> selectRefreshParentSession(Map<String, Object> parameters);

    List<Object> selectApplicationMenusForProfile(Map<String, Object> parameters);

    List<Object> selectActiveParentSession(Map<String, Object> parameters);

    List<Object> selectSsoClientForUpdate(Map<String, Object> parameters);

    List<Object> selectApplicationRedirectUris(Map<String, Object> parameters);

    List<Object> selectUserApplicationRoleCodes(Map<String, Object> parameters);

    List<Object> selectUserApplicationPermissionCodes(Map<String, Object> parameters);

    int updateUserLastLogin(Map<String, Object> parameters);

    int insertSsoSession(Map<String, Object> parameters);

    int rotateSsoRefreshToken(Map<String, Object> parameters);

    List<Object> selectProvisionApplicationForUpdate(Map<String, Object> parameters);

    List<Object> selectProvisionTenantForUpdate(Map<String, Object> parameters);

    List<Object> selectTenantApplicationForUpdate(Map<String, Object> parameters);

    List<Object> selectAvailableTenantUser(Map<String, Object> parameters);

    List<Object> selectTenantApplicationCandidates(Map<String, Object> parameters);

    List<Object> selectTenantApplicationId(Map<String, Object> parameters);

    List<Object> selectProvisionApplicationPermissions(Map<String, Object> parameters);

    int updateTenantApplication(Map<String, Object> parameters);

    int insertTenantApplication(Map<String, Object> parameters);

    int insertTenantApplicationAdminRole(Map<String, Object> parameters);

    int insertTenantApplicationRolePermission(Map<String, Object> parameters);

    int insertTenantApplicationUserRole(Map<String, Object> parameters);

    int insertTenantApplicationGrant(Map<String, Object> parameters);

    int activateTenantApplication(Map<String, Object> parameters);

    List<Object> catalogCount(Map<String, Object> parameters);

    List<Object> catalogPage(Map<String, Object> parameters);

    List<Object> tenantApplicationCount(Map<String, Object> parameters);

    List<Object> tenantApplicationPage(Map<String, Object> parameters);

    List<Object> tenantApplicationMine(Map<String, Object> parameters);

    List<Object> sessionCount(Map<String, Object> parameters);

    List<Object> sessionPage(Map<String, Object> parameters);

    List<Object> auditCount(Map<String, Object> parameters);

    List<Object> auditPage(Map<String, Object> parameters);

    List<Object> applicationPermissions(Map<String, Object> parameters);

    List<Object> applicationMenus(Map<String, Object> parameters);

    List<Object> applicationPermission(Map<String, Object> parameters);

    List<Object> applicationMenu(Map<String, Object> parameters);

    int deleteApplicationPermission(Map<String, Object> parameters);

    int deleteApplicationMenu(Map<String, Object> parameters);

    int logoutSession(Map<String, Object> parameters);

    int logoutGlobally(Map<String, Object> parameters);

    List<Object> consoleInstanceCount(Map<String, Object> parameters);

    int insertConsoleInstance(Map<String, Object> parameters);

    int insertSsoAudit(Map<String, Object> parameters);

    int revokeByApplication(Map<String, Object> parameters);

    int revokeByInstance(Map<String, Object> parameters);

    int revokeByUser(Map<String, Object> parameters);

    int revokeByTenant(Map<String, Object> parameters);

    int revokeByMember(Map<String, Object> parameters);
}
