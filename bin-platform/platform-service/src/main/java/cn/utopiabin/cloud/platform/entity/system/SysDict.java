package cn.utopiabin.cloud.platform.entity.system;

import cn.utopiabin.cloud.common.model.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统字典
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {
    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典编码 (唯一)
     */
    private String code;

    /**
     * 字典描述
     */
    private String comment;

    /**
     * 排序码
     */
    private Integer sort;

    /**
     * 是否启用
     */
    private Boolean available;
}
