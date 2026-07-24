package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.IdDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统字典 新增/编辑 DTO
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SysDictSaveDTO extends IdDTO {
    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典编码
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
