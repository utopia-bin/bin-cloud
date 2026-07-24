package cn.utopiabin.cloud.platform.model.dto.system;

import lombok.Data;

/**
 * 系统参数列表查询 DTO
 *
 * @since 1.0
 */
@Data
public class SysParameterListQuery {
    /**
     * 搜索关键字
     */
    private String keyword;
}
