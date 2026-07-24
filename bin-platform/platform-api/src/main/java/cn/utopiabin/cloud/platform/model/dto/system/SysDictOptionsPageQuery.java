package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictOptionsPageQuery extends PageQuery {
    /**
     * 所属字典 ID
     */
    private Long dictId;
}
