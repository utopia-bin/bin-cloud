package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictOptionsPageQuery;
import cn.utopiabin.cloud.platform.entity.system.SysDictOptions;
import cn.utopiabin.cloud.platform.mapper.system.SysDictOptionsMapper;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 系统字典项 Repository（直接供 ApiImpl 调用）
 *
 * @since 1.0
 */
@Repository
public class SysDictOptionsRepository extends ServiceImpl<SysDictOptionsMapper, SysDictOptions> {

    /**
     * 查单个，不存在则抛异常
     */
    public SysDictOptions getOrThrow(Long id) {
        SysDictOptions e = getById(id);
        if (e == null) {
            throw new BizException("字典项不存在");
        }
        return e;
    }

    /**
     * 同字典下名称或值重复计数
     */
    public long countByNameOrValue(long dictId, String name, String value, long excludeId) {
        return count(new LambdaQueryWrapper<SysDictOptions>()
                .and(qw -> qw.eq(SysDictOptions::getOptionName, name).or().eq(SysDictOptions::getOptionValue, value))
                .eq(SysDictOptions::getDictId, dictId)
                .ne(SysDictOptions::getId, excludeId));
    }

    /**
     * 分页
     */
    public Page<SysDictOptions> page(SysDictOptionsPageQuery query) {
        return page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysDictOptions>()
                        .eq(Objects.nonNull(query.getDictId()), SysDictOptions::getDictId, query.getDictId())
                        .orderByAsc(SysDictOptions::getParentId)
                        .orderByAsc(SysDictOptions::getSort)
                        .orderByAsc(SysDictOptions::getId));
    }

    /**
     * 按字典 ID 查列表
     */
    public List<SysDictOptions> listByDictId(Long dictId) {
        return list(new LambdaQueryWrapper<SysDictOptions>()
                .eq(dictId != null, SysDictOptions::getDictId, dictId)
                .orderByAsc(SysDictOptions::getParentId)
                .orderByAsc(SysDictOptions::getSort));
    }

    /**
     * 按字典 ID 删除所有字典项
     */
    public void removeByDictId(long dictId) {
        remove(new LambdaQueryWrapper<SysDictOptions>().eq(SysDictOptions::getDictId, dictId));
    }

    /**
     * 是否有子项
     */
    public boolean hasChild(long parentId) {
        return count(new LambdaQueryWrapper<SysDictOptions>().eq(SysDictOptions::getParentId, parentId)) > 0;
    }

    /**
     * 查询缓存列表 (携带字典编码)
     */
    public List<SysDictOptionsItemVO> listWithCode(String dictCode) {
        return baseMapper.listWithCode(dictCode);
    }
}
