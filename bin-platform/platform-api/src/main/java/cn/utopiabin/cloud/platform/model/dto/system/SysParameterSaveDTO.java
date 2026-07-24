package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.IdDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统参数 新增/编辑 DTO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SysParameterSaveDTO extends IdDTO {
    /**
     * 参数键
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
