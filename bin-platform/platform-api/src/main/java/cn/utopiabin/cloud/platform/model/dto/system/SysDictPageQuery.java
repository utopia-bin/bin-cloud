package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 字典分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictPageQuery extends PageQuery {
    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典编码
     */
    private String code;

    /**
     * 是否启用
     */
    private Boolean available;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
