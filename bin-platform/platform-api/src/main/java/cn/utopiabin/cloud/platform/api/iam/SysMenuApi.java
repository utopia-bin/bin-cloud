package cn.utopiabin.cloud.platform.api.iam;

import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 系统菜单 Dubbo API
 * <p>
 * 菜单管理，支持目录/菜单/按钮三种类型，提供树形结构查询。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "系统菜单", description = "菜单管理，含CRUD、批量删除、树形结构查询")
public interface SysMenuApi {

    @Operation(summary = "新增菜单", description = "上级菜单不能是自身")
    Long create(@Parameter(description = "菜单新增参数", required = true) @Valid SysMenuCreateDTO dto);

    @Operation(summary = "编辑菜单", description = "按ID编辑菜单信息")
    void update(@Parameter(description = "菜单编辑参数", required = true) @Valid SysMenuUpdateDTO dto);

    @Operation(summary = "删除菜单", description = "存在子级菜单时不可删除，同时清除关联的角色菜单记录")
    void remove(@Parameter(description = "菜单ID", required = true) Long id);

    @Operation(summary = "批量删除菜单", description = "批量删除菜单（需确保无子级）")
    void batchDelete(@Parameter(description = "批量删除参数", required = true) @Valid BatchDeleteDTO dto);

    @Operation(summary = "查询菜单详情")
    SysMenuVO get(@Parameter(description = "菜单ID", required = true) Long id);

    @Operation(summary = "列表查询菜单")
    List<SysMenuVO> list(@Parameter(description = "列表查询条件") SysMenuListQuery query);

    @Operation(summary = "获取菜单树", description = "按父子关系构建完整菜单树形结构")
    List<SysMenuTreeVO> tree(@Parameter(description = "列表查询条件") SysMenuListQuery query);

    @Operation(summary = "根据权限码获取菜单", description = "菜单仅作为权限的导航投影")
    List<SysMenuVO> listByPermissionCodes(
            @Parameter(description = "权限码列表", required = true) List<String> permissionCodes);
}
