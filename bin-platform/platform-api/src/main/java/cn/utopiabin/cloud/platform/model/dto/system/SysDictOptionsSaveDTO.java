package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.IdDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 字典项 新增/编辑 DTO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SysDictOptionsSaveDTO extends IdDTO {
    /**
     * 父级字典项 ID
     */
    private Long parentId;

    /**
     * 所属字典 ID
     */
    private Long dictId;

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
