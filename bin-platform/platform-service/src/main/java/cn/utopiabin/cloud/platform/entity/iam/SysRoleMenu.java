package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.platform.entity.base.LinkEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 角色菜单关联
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
@Schema(description = "角色菜单关联")
public class SysRoleMenu extends LinkEntity {

    /**
     * 角色 ID
     */
    @Schema(description = "角色ID")
    private Long roleId;

    /**
     * 菜单 ID
     */
    @Schema(description = "菜单ID")
    private Long menuId;
}
