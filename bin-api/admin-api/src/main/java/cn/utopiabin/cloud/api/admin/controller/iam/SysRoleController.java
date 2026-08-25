package cn.utopiabin.cloud.api.admin.controller.iam;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.iam.SysRoleApi;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleAssignPermissionsDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRolePageQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

/** 系统角色管理 REST 接口。 */
@Tag(name = "系统角色")
@Validated
@RestController
@RequestMapping("/roles")
public class SysRoleController {

    @DubboReference
    private SysRoleApi roleApi;

    @Operation(summary = "新增角色")
    @PostMapping
    public RestResult<Long> create(@Valid @RequestBody SysRoleCreateDTO dto) {
        return RestResult.ok(roleApi.create(dto));
    }

    @Operation(summary = "编辑角色")
    @PutMapping
    public RestResult<Void> update(@Valid @RequestBody SysRoleUpdateDTO dto) {
        roleApi.update(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public RestResult<Void> remove(@PathVariable Long id) {
        roleApi.remove(id);
        return RestResult.ok();
    }

    @Operation(summary = "批量删除角色")
    @DeleteMapping
    public RestResult<Void> batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        roleApi.batchDelete(dto);
        return RestResult.ok();
    }

    @Operation(summary = "启用或禁用角色")
    @PatchMapping("/{id}/available")
    public RestResult<Void> enable(@PathVariable Long id, @RequestParam Boolean available) {
        roleApi.enable(id, available);
        return RestResult.ok();
    }

    @Operation(summary = "查询角色详情")
    @GetMapping("/{id}")
    public RestResult<SysRoleVO> get(@PathVariable Long id) {
        return RestResult.ok(roleApi.get(id));
    }

    @Operation(summary = "分页查询角色")
    @GetMapping
    public RestResult<PageResult<SysRoleVO>> page(@Valid @ModelAttribute SysRolePageQuery query) {
        return RestResult.ok(roleApi.page(query));
    }

    @Operation(summary = "列表查询角色")
    @GetMapping("/list")
    public RestResult<List<SysRoleVO>> list(@Valid @ModelAttribute SysRoleListQuery query) {
        return RestResult.ok(roleApi.list(query));
    }

    @Operation(summary = "为角色分配权限")
    @PutMapping("/{id}/permissions")
    public RestResult<Void> assignPermissions(@PathVariable Long id,
                                              @Valid @RequestBody AssignPermissionsRequest request) {
        var dto = new SysRoleAssignPermissionsDTO();
        dto.setRoleId(id);
        dto.setPermissionIds(request.permissionIds());
        dto.setExpectedVersion(request.expectedVersion());
        roleApi.assignPermissions(dto);
        return RestResult.ok();
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    public RestResult<List<SysPermissionVO>> getPermissions(@PathVariable Long id) {
        return RestResult.ok(roleApi.getPermissions(id));
    }

    @Operation(summary = "检查角色编码是否存在")
    @GetMapping("/exists")
    public RestResult<Boolean> existsByCode(@RequestParam String code) {
        return RestResult.ok(roleApi.existsByCode(code));
    }

    @Schema(description = "角色全量分配权限参数")
    public record AssignPermissionsRequest(
            @NotNull(message = "权限ID列表不能为空")
            @Schema(description = "替换后的权限 ID 列表；空列表表示清除角色的全部权限", example = "[1, 2]",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            List<Long> permissionIds,
            @NotNull(message = "角色版本号不能为空")
            @Schema(description = "客户端读取到的角色版本号，用于乐观并发控制", example = "1",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Integer expectedVersion) {
    }
}
