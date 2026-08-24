package cn.utopiabin.cloud.platform.model.vo.iam;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户权限聚合 VO
 * <p>
 * 缓存对象，包含用户角色和菜单的完整信息，避免每次请求多次查库。
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户权限聚合")
public class UserPermissionVO extends JsonSerializable {

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "角色列表")
    private List<SysRoleVO> roles;

    @Schema(description = "权限码列表")
    private List<String> permissionCodes;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;

    @Schema(description = "菜单列表")
    private List<SysMenuVO> menus;

    @Schema(description = "菜单树")
    private List<SysMenuTreeVO> menuTree;
}
