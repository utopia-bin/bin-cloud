package cn.utopiabin.cloud.platform.repository.base;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * Repository 抽象基类
 * <p>
 * 封装通用查询方法，消除各 Repository 中的重复代码。
 * 仅适用于主表实体 (extends {@link BaseEntity})，关联表请使用 {@code ServiceImpl}。
 *
 * @param <M> Mapper 类型
 * @param <T> 实体类型
 * @since 1.0
 */
public abstract class BaseRepository<M extends BaseMapper<T>, T extends BaseEntity>
        extends ServiceImpl<M, T> {

    /**
     * 按 ID 查询，不存在则抛出 {@link BizException}
     *
     * @param id 主键 ID
     * @return 实体
     */
    public T getOrThrow(Long id) {
        T entity = getById(id);
        if (entity == null) {
            throw new BizException(PlatformErrorCode.NOT_FOUND.getCode(),
                    getNotFoundMessage());
        }
        return entity;
    }

    /**
     * 判断指定字段值是否存在
     *
     * @param field 实体字段引用 (如 SysUser::getUsername)
     * @param value 字段值
     * @return true=存在
     */
    public boolean exists(SFunction<T, ?> field, Object value) {
        return count(new LambdaQueryWrapper<T>().eq(field, value)) > 0;
    }

    /**
     * 按字段值计数 (排除指定 ID，用于唯一性校验)
     *
     * @param field      实体字段引用
     * @param value      字段值
     * @param excludeId  排除的 ID (编辑时传当前实体 ID，新增时传 null)
     * @return 计数
     */
    public long countByField(SFunction<T, ?> field, Object value, Long excludeId) {
        return count(new LambdaQueryWrapper<T>()
                .eq(field, value)
                .ne(excludeId != null && excludeId > 0, BaseEntity::getId, excludeId));
    }

    /**
     * 资源不存在时的提示消息，子类可覆盖
     */
    protected String getNotFoundMessage() {
        return "数据不存在";
    }
}
