package cn.utopiabin.cloud.platform.entity.base;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 关联表基类 —— 仅含主键，不含租户/审计字段
 * <p>
 * 适用于 sys_role_menu、sys_user_role 等纯关联表
 *
 * @since 1.0
 */
@Getter
@Setter
public abstract class LinkEntity extends JsonSerializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
}
