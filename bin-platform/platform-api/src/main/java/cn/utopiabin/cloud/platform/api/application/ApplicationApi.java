package cn.utopiabin.cloud.platform.api.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationResourceDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationRoleDTO;
import cn.utopiabin.cloud.platform.model.dto.application.InstanceDTO;
import cn.utopiabin.cloud.platform.model.dto.application.UserGrantDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationResourceVO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationRoleVO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.ClientSecretVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoAuditVO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoSessionVO;
import cn.utopiabin.cloud.platform.model.vo.application.TenantApplicationVO;
import cn.utopiabin.cloud.platform.model.vo.application.UserApplicationVO;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import java.util.List;

public interface ApplicationApi {
    @Operation(summary = "应用产品分页")
    PageResult<ApplicationVO> applications(@Valid ApplicationQuery query) throws BizException;

    @Operation(summary = "应用详情与回调白名单")
    ApplicationVO application(long id) throws BizException;

    @Operation(summary = "新增或编辑应用")
    long saveApplication(@Valid ApplicationDTO dto) throws BizException;

    @Operation(summary = "删除已关闭的应用")
    void removeApplication(long id, int version) throws BizException;

    @Operation(summary = "轮换后端客户端凭证")
    ClientSecretVO rotateClientSecret(long id, int version) throws BizException;

    @Operation(summary = "租户开通实例分页")
    PageResult<TenantApplicationVO> instances(@Valid ApplicationQuery query) throws BizException;

    @Operation(summary = "开通、暂停、关闭或恢复")
    long saveInstance(@Valid InstanceDTO dto) throws BizException;

    @Operation(summary = "当前用户可进入应用")
    List<TenantApplicationVO> mine() throws BizException;

    @Operation(summary = "开通管理员候选用户")
    List<UserApplicationVO> candidates(long tenantId) throws BizException;

    @Operation(summary = "应用成员与角色")
    List<UserApplicationVO> members(long instanceId) throws BizException;

    @Operation(summary = "设置成员准入与角色")
    void grant(@Valid UserGrantDTO dto) throws BizException;

    @Operation(summary = "应用实例角色")
    List<ApplicationRoleVO> roles(long instanceId) throws BizException;

    @Operation(summary = "保存应用角色权限")
    long saveRole(@Valid ApplicationRoleDTO dto) throws BizException;

    @Operation(summary = "删除应用角色")
    void removeRole(long instanceId, long id, int version) throws BizException;

    @Operation(summary = "应用菜单或权限目录")
    List<ApplicationResourceVO> resources(long applicationId, String kind) throws BizException;

    @Operation(summary = "保存应用资源")
    long saveResource(String kind, @Valid ApplicationResourceDTO dto) throws BizException;

    @Operation(summary = "删除应用资源")
    void removeResource(long applicationId, String kind, long id, int version) throws BizException;

    @Operation(summary = "会话分页")
    PageResult<SsoSessionVO> sessions(@Valid ApplicationQuery query) throws BizException;

    @Operation(summary = "SSO审计分页")
    PageResult<SsoAuditVO> audit(@Valid ApplicationQuery query) throws BizException;

    @Operation(summary = "强制下线")
    void revokeSession(String sessionId) throws BizException;
}
