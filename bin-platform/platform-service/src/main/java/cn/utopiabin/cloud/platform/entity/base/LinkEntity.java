package cn.utopiabin.cloud.platform.entity.base;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 租户关联表基类
 *
 * <p>关联数据也必须携带租户 ID，避免仅依赖两端主表的租户过滤形成跨租户脏关联。
 *
 * @since 1.0
 */
@Getter
@Setter
public abstract class LinkEntity extends JsonSerializable {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    @Schema(description = "授权关系所属租户应用实例")
    private Long tenantApplicationId;
}
