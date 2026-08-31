package cn.utopiabin.cloud.api.admin.controller.application;

import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.application.*;
import cn.utopiabin.cloud.platform.model.vo.application.*;
import cn.utopiabin.cloud.platform.api.application.ApplicationApi;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Validated
@RestController
@RequestMapping("/applications")
@Tag(name="应用开通管理")
public class ApplicationController {
    @DubboReference private ApplicationApi api;

    @GetMapping("/catalog")
    @Operation(summary="分页查询应用产品目录")
    public RestResult<PageResult<ApplicationVO>> catalog(@Valid @ModelAttribute ApplicationQuery query) { return RestResult.ok(api.applications(query)); }

    @GetMapping("/catalog/{id}")
    @Operation(summary="查询应用详情及回调白名单")
    public RestResult<ApplicationVO> get(@PathVariable long id) { return RestResult.ok(api.application(id)); }

    @PostMapping("/catalog")
    @Operation(summary="新增或编辑应用产品")
    public RestResult<Long> save(@Valid @RequestBody ApplicationDTO dto) { return RestResult.ok(api.saveApplication(dto)); }

    @DeleteMapping("/catalog/{id}")
    @Operation(summary="删除已关闭全部实例的应用")
    public RestResult<Void> remove(@PathVariable long id,@RequestParam int version) { api.removeApplication(id,version); return RestResult.ok(); }

    @PostMapping("/catalog/{id}/client-secret")
    @Operation(summary="轮换后端客户端凭证，仅返回一次明文")
    public RestResult<ClientSecretVO> secret(@PathVariable long id,@RequestParam int version) { return RestResult.ok(api.rotateClientSecret(id,version)); }

    @GetMapping("/instances")
    @Operation(summary="分页查询租户应用实例")
    public RestResult<PageResult<TenantApplicationVO>> instances(@Valid @ModelAttribute ApplicationQuery query) { return RestResult.ok(api.instances(query)); }

    @PostMapping("/instances")
    @Operation(summary="开通、暂停、关闭或恢复租户应用")
    public RestResult<Long> provision(@Valid @RequestBody InstanceDTO dto) { return RestResult.ok(api.saveInstance(dto)); }

    @GetMapping("/mine")
    @Operation(summary="查询当前用户可进入的应用")
    public RestResult<List<TenantApplicationVO>> mine() { return RestResult.ok(api.mine()); }

    @GetMapping("/candidates")
    @Operation(summary="查询租户内可用的应用管理员候选账号")
    public RestResult<List<UserApplicationVO>> candidates(@RequestParam long tenantId) { return RestResult.ok(api.candidates(tenantId)); }

    @GetMapping("/instances/{instanceId}/members")
    @Operation(summary="查询实例成员准入与角色")
    public RestResult<List<UserApplicationVO>> members(@PathVariable long instanceId) { return RestResult.ok(api.members(instanceId)); }

    @PostMapping("/members")
    @Operation(summary="设置成员准入及角色")
    public RestResult<Void> grant(@Valid @RequestBody UserGrantDTO dto) { api.grant(dto); return RestResult.ok(); }

    @GetMapping("/instances/{instanceId}/roles")
    @Operation(summary="查询当前实例的应用角色")
    public RestResult<List<ApplicationRoleVO>> roles(@PathVariable long instanceId) { return RestResult.ok(api.roles(instanceId)); }

    @PostMapping("/roles")
    @Operation(summary="新增或编辑应用角色与权限")
    public RestResult<Long> saveRole(@Valid @RequestBody ApplicationRoleDTO dto) { return RestResult.ok(api.saveRole(dto)); }

    @DeleteMapping("/instances/{instanceId}/roles/{id}")
    @Operation(summary="删除应用角色并撤销关联授权")
    public RestResult<Void> removeRole(@PathVariable long instanceId,@PathVariable long id,@RequestParam int version) { api.removeRole(instanceId,id,version); return RestResult.ok(); }

    @GetMapping("/catalog/{applicationId}/resources/{kind}")
    @Operation(summary="查询指定应用的权限或菜单")
    public RestResult<List<ApplicationResourceVO>> resources(@PathVariable long applicationId,@PathVariable String kind) { return RestResult.ok(api.resources(applicationId,kind)); }

    @PostMapping("/resources/{kind}")
    @Operation(summary="新增或编辑应用权限及菜单")
    public RestResult<Long> saveResource(@PathVariable String kind,@Valid @RequestBody ApplicationResourceDTO dto) { return RestResult.ok(api.saveResource(kind,dto)); }

    @DeleteMapping("/catalog/{applicationId}/resources/{kind}/{id}")
    @Operation(summary="删除未被引用的应用资源")
    public RestResult<Void> removeResource(@PathVariable long applicationId,@PathVariable String kind,@PathVariable long id,@RequestParam int version) { api.removeResource(applicationId,kind,id,version); return RestResult.ok(); }

    @GetMapping("/sessions")
    @Operation(summary="分页查询应用登录会话")
    public RestResult<PageResult<SsoSessionVO>> sessions(@Valid @ModelAttribute ApplicationQuery query) { return RestResult.ok(api.sessions(query)); }

    @GetMapping("/audit")
    @Operation(summary="分页查询单点登录审计")
    public RestResult<PageResult<SsoAuditVO>> audit(@Valid @ModelAttribute ApplicationQuery query) { return RestResult.ok(api.audit(query)); }

    @PostMapping("/sessions/{sessionId}/revoke")
    @Operation(summary="撤销会话及其关联子会话")
    public RestResult<Void> revoke(@PathVariable String sessionId) { api.revokeSession(sessionId); return RestResult.ok(); }
}
