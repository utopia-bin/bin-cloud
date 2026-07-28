package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.platform.entity.base.LinkEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户角色关联
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
@Schema(description = "用户角色关联")
public class SysUserRole extends LinkEntity {

    /**
     * 用户 ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 角色 ID
     */
    @Schema(description = "角色ID")
    private Long roleId;
}
