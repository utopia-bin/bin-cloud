package cn.utopiabin.cloud.common.model.dto;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询请求参数基类
 *
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageQuery extends JsonSerializable {
    /**
     * 当前页码 (从 1 开始, 默认 1)
     */
    private long page = 1;

    /**
     * 每页条数 (默认 10, 最大 1000)
     */
    private long size = 10;

    public long getPage() {
        return Math.max(1, page);
    }

    public long getSize() {
        return Math.clamp(size, 1, 1000);
    }

    public long offset() {
        return (getPage() - 1) * getSize();
    }
}
