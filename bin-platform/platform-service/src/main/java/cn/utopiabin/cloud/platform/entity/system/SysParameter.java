package cn.utopiabin.cloud.platform.entity.system;

import cn.utopiabin.cloud.common.model.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统参数
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_parameter")
public class SysParameter extends BaseEntity {
    /**
     * 参数键 (唯一)
     */
    private String paramKey;

    /**
     * 参数值
     */
    private String paramValue;

    /**
     * 参数描述
     */
    private String paramComment;

    /**
     * 排序码
     */
    private Integer sort;
}
