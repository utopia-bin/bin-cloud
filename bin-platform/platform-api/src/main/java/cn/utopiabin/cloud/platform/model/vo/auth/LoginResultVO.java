package cn.utopiabin.cloud.platform.model.vo.auth;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 登录结果 VO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录结果")
public class LoginResultVO extends JsonSerializable {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户信息")
    private SysUserVO user;

    @Schema(description = "角色列表")
    private List<SysRoleVO> roles;

    @Schema(description = "菜单树（前端动态路由）")
    private List<SysMenuTreeVO> menus;
}
