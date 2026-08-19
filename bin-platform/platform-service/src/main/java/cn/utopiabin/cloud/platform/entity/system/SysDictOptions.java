package cn.utopiabin.cloud.platform.entity.system;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统字典项
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_options")
public class SysDictOptions extends BaseEntity {
    /**
     * 所属字典 ID
     */
    private Long dictId;

    /**
     * 父级字典项 ID
     */
    private Long parentId;

    /**
     * 字典项名称
     */
    private String optionName;

    /**
     * 字典项值
     */
    private String optionValue;

    /**
     * 字典项描述
     */
    private String optionComment;

    /**
     * 排序码
     */
    private Integer sort;
}
