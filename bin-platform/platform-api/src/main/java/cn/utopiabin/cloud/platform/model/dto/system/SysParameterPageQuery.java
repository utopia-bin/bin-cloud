package cn.utopiabin.cloud.platform.model.dto.system;

import cn.utopiabin.cloud.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数分页查询 DTO
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysParameterPageQuery extends PageQuery {
    /**
     * 搜索关键字 (匹配 paramKey / paramValue / paramComment)
     */
    private String keyword;
}
