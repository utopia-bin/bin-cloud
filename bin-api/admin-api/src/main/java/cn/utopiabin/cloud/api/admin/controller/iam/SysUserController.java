package cn.utopiabin.cloud.api.admin.controller.iam;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.iam.SysUserApi;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserAssignRolesDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserPageQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统用户管理 REST 接口。 */
@Tag(name = "系统用户")
@Validated
@RestController
@RequestMapping("/users")
public class SysUserController {

    @DubboReference
    private SysUserApi userApi;

    @Operation(summary = "新增用户")
    @PostMapping
    public RestResult<Long> create(@Valid @RequestBody SysUserCreateDTO dto) {
        return RestResult.ok(userApi.create(dto));
    }

    @Operation(summary = "编辑用户")
    @PutMapping
    public RestResult<Void> update(@Valid @RequestBody SysUserUpdateDTO dto) {
        userApi.update(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public RestResult<Void> remove(@PathVariable Long id) {
        userApi.remove(id);
        return RestResult.ok();
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping
    public RestResult<Void> batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        userApi.batchDelete(dto);
        return RestResult.ok();
    }

    @Operation(summary = "启用或禁用用户")
    @PatchMapping("/{id}/available")
    public RestResult<Void> enable(@PathVariable Long id, @RequestParam Boolean available) {
        userApi.enable(id, available);
        return RestResult.ok();
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public RestResult<SysUserVO> get(@PathVariable Long id) {
        return RestResult.ok(userApi.get(id));
    }

    @Operation(summary = "分页查询用户")
    @GetMapping
    public RestResult<PageResult<SysUserVO>> page(@Valid @ModelAttribute SysUserPageQuery query) {
        return RestResult.ok(userApi.page(query));
    }

    @Operation(summary = "列表查询用户")
    @GetMapping("/list")
    public RestResult<List<SysUserVO>> list(@Valid @ModelAttribute SysUserListQuery query) {
        return RestResult.ok(userApi.list(query));
    }

    @Operation(summary = "为用户分配角色")
    @PutMapping("/{id}/roles")
    public RestResult<Void> assignRoles(@PathVariable Long id,
                                        @Valid @RequestBody AssignRolesRequest request) {
        var dto = new SysUserAssignRolesDTO();
        dto.setUserId(id);
        dto.setRoleIds(request.roleIds());
        dto.setExpectedVersion(request.expectedVersion());
        userApi.assignRoles(dto);
        return RestResult.ok();
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/{id}/roles")
    public RestResult<List<SysRoleVO>> getRoles(@PathVariable Long id) {
        return RestResult.ok(userApi.getRoles(id));
    }

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/exists")
    public RestResult<Boolean> existsByUsername(@RequestParam String username) {
        return RestResult.ok(userApi.existsByUsername(username));
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    public RestResult<Void> resetPassword(@PathVariable Long id,
                                          @Valid @RequestBody ResetPasswordRequest request) {
        userApi.resetPassword(id, request.newPassword());
        return RestResult.ok();
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "新密码不能为空")
            @Size(min = 8, max = 64, message = "新密码长度必须在8到64个字符之间")
            String newPassword) {
    }

    public record AssignRolesRequest(
            @NotNull(message = "角色ID列表不能为空") List<Long> roleIds,
            @NotNull(message = "用户版本号不能为空") Integer expectedVersion) {
    }
}
