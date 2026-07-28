package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.system.SysDict;
import cn.utopiabin.cloud.platform.mapper.system.SysDictMapper;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysDictPageQuery;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 系统字典 Repository
 *
 * @since 1.0
 */
@Repository
public class SysDictRepository extends BaseRepository<SysDictMapper, SysDict> {

    @Override
    protected String getNotFoundMessage() {
        return "字典不存在";
    }

    /**
     * 仅查字典编码（轻量查询，仅 SELECT code）
     *
     * @param id 字典ID
     * @return 字典编码，不存在则抛异常
     */
    public String getCodeOrThrow(Long id) {
        SysDict dict = getOne(new LambdaQueryWrapper<SysDict>()
                .select(SysDict::getCode)
                .eq(SysDict::getId, id));
        if (dict == null) {
            return null;
        }
        return dict.getCode();
    }

    /**
     * 名称或编码重复计数（排除指定 ID）
     *
     * @param name       字典名称
     * @param code       字典编码
     * @param excludeId  排除的 ID（新增传 null，编辑传当前 ID）
     * @return 计数
     */
    public long countByNameOrCode(String name, String code, Long excludeId) {
        return count(new LambdaQueryWrapper<SysDict>()
                .and(qw -> qw.eq(SysDict::getName, name).or().eq(SysDict::getCode, code))
                .ne(excludeId != null && excludeId > 0, SysDict::getId, excludeId));
    }

    /**
     * 分页查询
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
     * 列表查询
     */
    public List<SysDict> list(SysDictListQuery query) {
        return list(new LambdaQueryWrapper<SysDict>()
                .eq(query != null && Objects.nonNull(query.getAvailable()), SysDict::getAvailable, query != null ? query.getAvailable() : null)
                .orderByAsc(SysDict::getSort)
                .orderByDesc(SysDict::getId));
    }
}
