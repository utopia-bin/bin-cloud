package cn.utopiabin.cloud.platform.entity.base;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 实体公共基类
 *
 * @author Bin
 * @since 1.0
 */
@Getter
@Setter
public abstract class BaseEntity extends JsonSerializable {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 逻辑删除标识 (0-未删除 1-已删除)
     */
    @TableLogic
    @JsonIgnore
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModify;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    /**
     * 修改人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String modifyUser;
}
