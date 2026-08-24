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
 * 系统用户 Dubbo API
 * <p>
 * 用户管理，含CRUD、批量删除、启禁用、角色分配、密码重置等。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "系统用户", description = "用户管理，含CRUD、批量删除、启禁用、角色分配、密码重置")
public interface SysUserApi {

    @Operation(summary = "新增用户", description = "用户名唯一校验，密码BCrypt加密存储")
    Long create(@Parameter(description = "用户新增参数", required = true) SysUserCreateDTO dto);

    @Operation(summary = "编辑用户", description = "按ID编辑用户信息，密码不填则不修改")
    void update(@Parameter(description = "用户编辑参数", required = true) SysUserUpdateDTO dto);

    @Operation(summary = "删除用户", description = "逻辑删除，同时删除该用户的所有角色关联")
    void remove(@Parameter(description = "用户ID", required = true) Long id);

    @Operation(summary = "批量删除用户", description = "批量逻辑删除，同时删除关联的角色记录")
    void batchDelete(@Parameter(description = "批量删除参数", required = true) BatchDeleteDTO dto);

    @Operation(summary = "启用/禁用用户", description = "切换用户的启用状态")
    void enable(@Parameter(description = "用户ID", required = true) Long id,
                @Parameter(description = "是否启用", required = true) Boolean available);

    @Operation(summary = "查询用户详情", description = "返回用户信息，不含密码字段")
    SysUserVO get(@Parameter(description = "用户ID", required = true) Long id);

    @Operation(summary = "分页查询用户")
    PageResult<SysUserVO> page(@Parameter(description = "分页查询条件", required = true) SysUserPageQuery query);

    @Operation(summary = "列表查询用户")
    List<SysUserVO> list(@Parameter(description = "列表查询条件") SysUserListQuery query);

    @Operation(summary = "为用户分配角色", description = "全量替换用户的角色列表")
    void assignRoles(@Parameter(description = "用户分配角色参数", required = true) SysUserAssignRolesDTO dto);

    @Operation(summary = "获取用户拥有的角色列表")
    List<SysRoleVO> getRoles(@Parameter(description = "用户ID", required = true) Long userId);

    @Operation(summary = "检查用户名是否存在", description = "用于新增/编辑时的唯一性校验")
    boolean existsByUsername(@Parameter(description = "用户名", required = true) String username);

    @Operation(summary = "重置用户密码", description = "管理员重置用户密码，无需原密码")
    void resetPassword(@Parameter(description = "用户ID", required = true) Long userId,
                       @Parameter(description = "新密码", required = true) String newPassword);
}
