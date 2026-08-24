package cn.utopiabin.cloud.platform.api.iam;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 系统角色 Dubbo API
 * <p>
 * 角色管理，角色绑定权限资源，菜单仅作为权限的导航投影。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "系统角色", description = "角色管理，含CRUD、批量删除、启禁用、权限分配")
public interface SysRoleApi {

    @Operation(summary = "新增角色", description = "角色编码全局唯一校验")
    Long create(@Parameter(description = "角色新增参数", required = true) SysRoleCreateDTO dto);

    @Operation(summary = "编辑角色", description = "按ID编辑角色信息")
    void update(@Parameter(description = "角色编辑参数", required = true) SysRoleUpdateDTO dto);

    @Operation(summary = "删除角色", description = "同时删除该角色的所有菜单关联和用户关联")
    void remove(@Parameter(description = "角色ID", required = true) Long id);

    @Operation(summary = "批量删除角色", description = "批量删除角色及其关联记录")
    void batchDelete(@Parameter(description = "批量删除参数", required = true) BatchDeleteDTO dto);

    @Operation(summary = "启用/禁用角色", description = "切换角色的启用状态")
    void enable(@Parameter(description = "角色ID", required = true) Long id,
                @Parameter(description = "是否启用", required = true) Boolean available);

    @Operation(summary = "查询角色详情")
    SysRoleVO get(@Parameter(description = "角色ID", required = true) Long id);

    @Operation(summary = "分页查询角色")
    PageResult<SysRoleVO> page(@Parameter(description = "分页查询条件", required = true) SysRolePageQuery query);

    @Operation(summary = "列表查询角色")
    List<SysRoleVO> list(@Parameter(description = "列表查询条件") SysRoleListQuery query);

    @Operation(summary = "为角色分配权限", description = "全量替换角色权限并校验乐观锁版本")
    void assignPermissions(
            @Parameter(description = "角色分配权限参数", required = true) SysRoleAssignPermissionsDTO dto);

    @Operation(summary = "获取角色拥有的权限列表")
    List<SysPermissionVO> getPermissions(
            @Parameter(description = "角色ID", required = true) Long roleId);

    @Operation(summary = "检查角色编码是否存在", description = "用于新增/编辑时的唯一性校验")
    boolean existsByCode(@Parameter(description = "角色编码", required = true) String code);
}
