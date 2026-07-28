package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.common.model.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统角色
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@Schema(description = "系统角色")
public class SysRole extends BaseEntity {

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String name;

    /**
     * 角色编码 (唯一)
     */
    @Schema(description = "角色编码（唯一）")
    private String code;

    /**
     * 数据权限范围: 1全部 2本部门 3本部门及以下 4仅本人
     */
    @Schema(description = "数据权限范围: 1全部 2本部门 3本部门及以下 4仅本人")
    private Integer dataScope;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private Boolean available;

    /**
     * 排序码
     */
    @Schema(description = "排序码")
    private Integer sort;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String comment;
}
