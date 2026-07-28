package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.platform.entity.system.SysDictOptions;
import cn.utopiabin.cloud.platform.mapper.system.SysDictOptionsMapper;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictOptionsPageQuery;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsItemVO;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 系统字典项 Repository
 *
 * @since 1.0
 */
@Repository
public class SysDictOptionsRepository extends BaseRepository<SysDictOptionsMapper, SysDictOptions> {

    @Override
    protected String getNotFoundMessage() {
        return "字典项不存在";
    }

    /**
     * 同字典下名称或值重复计数（排除指定 ID）
     *
     * @param dictId     字典ID
     * @param name       字典项名称
     * @param value      字典项值
     * @param excludeId  排除的 ID（新增传 null，编辑传当前 ID）
     * @return 计数
     */
    public long countByNameOrValue(Long dictId, String name, String value, Long excludeId) {
        return count(new LambdaQueryWrapper<SysDictOptions>()
                .and(qw -> qw.eq(SysDictOptions::getOptionName, name).or().eq(SysDictOptions::getOptionValue, value))
                .eq(SysDictOptions::getDictId, dictId)
                .ne(excludeId != null && excludeId > 0, SysDictOptions::getId, excludeId));
    }

    /**
     * 分页查询
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
    public void removeByDictId(Long dictId) {
        remove(new LambdaQueryWrapper<SysDictOptions>().eq(SysDictOptions::getDictId, dictId));
    }

    /**
     * 是否有子项
     */
    public boolean hasChild(Long parentId) {
        return count(new LambdaQueryWrapper<SysDictOptions>().eq(SysDictOptions::getParentId, parentId)) > 0;
    }

    /**
     * 查询缓存列表 (携带字典编码)
     *
     * @param dictCode 字典编码，为 null 时查询全部
     * @return 字典项精简 VO 列表
     */
    public List<SysDictOptionsItemVO> listWithCode(String dictCode) {
        return baseMapper.listWithCode(dictCode);
    }
}
