package cn.utopiabin.cloud.common.model.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 分页查询统一返回包装类
 *
 * @param <T> 列表元素类型
 * @since 1.0
 */
@Getter
@Setter
@ToString
public class PageResult<T> {

    /**
     * 当前页码 (从 1 开始)
     */
    private long page;

    /**
     * 每页条数
     */
    private long size;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    private PageResult() {
    }

    /**
     * 构建分页结果
     *
     * @param page    当前页
     * @param size    每页条数
     * @param total   总记录数
     * @param records 数据列表
     */
    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        var result = new PageResult<T>();
        result.page = page;
        result.size = size;
        result.total = total;
        result.pages = size > 0 ? (total + size - 1) / size : 0;
        result.records = records != null ? records : List.of();
        return result;
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return of(1, 10, 0, List.of());
    }

    /**
     * 空分页结果 (指定 page/size)
     */
    public static <T> PageResult<T> empty(long page, long size) {
        return of(page, size, 0, List.of());
    }
}
