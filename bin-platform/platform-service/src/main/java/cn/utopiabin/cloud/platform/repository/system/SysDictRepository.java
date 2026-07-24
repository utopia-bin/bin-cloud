package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictPageQuery;
import cn.utopiabin.cloud.platform.entity.system.SysDict;
import cn.utopiabin.cloud.platform.mapper.system.SysDictMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 系统字典 Repository（直接供 ApiImpl 调用）
 *
 * @since 1.0
 */
@Repository
public class SysDictRepository extends ServiceImpl<SysDictMapper, SysDict> {

    /**
     * 查单个，不存在则抛异常
     */
    public SysDict getOrThrow(Long id) {
        SysDict e = getById(id);
        if (e == null) {
            throw new BizException("字典不存在");
        }
        return e;
    }

    /**
     * 仅查字典编码（轻量查询，仅 SELECT code，用于缓存刷新等无需全量字段的场景）
     */
    public String getCodeOrThrow(Long id) {
        SysDict dict = getOne(new LambdaQueryWrapper<SysDict>()
                .select(SysDict::getCode)
                .eq(SysDict::getId, id));
        if (dict == null) {
            throw new BizException("字典不存在");
        }
        return dict.getCode();
    }

    /**
     * 名称或编码重复计数
     */
    public long countByNameOrCode(String name, String code, long excludeId) {
        return count(new LambdaQueryWrapper<SysDict>()
                .and(qw -> qw.eq(SysDict::getName, name).or().eq(SysDict::getCode, code))
                .ne(SysDict::getId, excludeId));
    }

    /**
     * 分页
     */
    public Page<SysDict> page(SysDictPageQuery query) {
        return page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysDict>()
                        .like(StrUtil.isNotBlank(query.getName()), SysDict::getName, query.getName())
                        .like(StrUtil.isNotBlank(query.getCode()), SysDict::getCode, query.getCode())
                        .eq(Objects.nonNull(query.getAvailable()), SysDict::getAvailable, query.getAvailable())
                        .ge(Objects.nonNull(query.getStartTime()), SysDict::getGmtCreate, query.getStartTime())
                        .le(Objects.nonNull(query.getEndTime()), SysDict::getGmtCreate, query.getEndTime())
                        .orderByAsc(SysDict::getSort)
                        .orderByDesc(SysDict::getId));
    }

    /**
     * 列表
     */
    public List<SysDict> list(SysDictListQuery query) {
        return list(new LambdaQueryWrapper<SysDict>()
                .eq(Objects.nonNull(query.getAvailable()), SysDict::getAvailable, query.getAvailable())
                .orderByAsc(SysDict::getSort)
                .orderByDesc(SysDict::getId));
    }
}
