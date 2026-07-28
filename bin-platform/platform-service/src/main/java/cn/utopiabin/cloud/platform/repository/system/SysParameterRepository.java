package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.system.SysParameter;
import cn.utopiabin.cloud.platform.mapper.system.SysParameterMapper;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统参数 Repository
 *
 * @since 1.0
 */
@Repository
public class SysParameterRepository extends BaseRepository<SysParameterMapper, SysParameter> {

    @Override
    protected String getNotFoundMessage() {
        return "系统参数不存在";
    }

    /**
     * 参数键是否已存在（排除指定 ID）
     *
     * @param key        参数键
     * @param excludeId  排除的 ID（新增传 null，编辑传当前 ID）
     * @return true=已存在
     */
    public boolean keyExists(String key, Long excludeId) {
        return count(new LambdaQueryWrapper<SysParameter>()
                .eq(SysParameter::getParamKey, key)
                .ne(excludeId != null && excludeId > 0, SysParameter::getId, excludeId)) > 0;
    }

    /**
     * 按参数键查询
     */
    public SysParameter findByKey(String key) {
        return getOne(new LambdaQueryWrapper<SysParameter>()
                .eq(SysParameter::getParamKey, key)
                .orderByDesc(SysParameter::getSort)
                .last("LIMIT 1"));
    }

    /**
     * 列表查询
     */
    public List<SysParameter> list(SysParameterListQuery query) {
        return list(new LambdaQueryWrapper<SysParameter>()
                .and(query != null && StrUtil.isNotBlank(query.getKeyword()), q -> q
                        .like(SysParameter::getParamKey, query != null ? query.getKeyword() : null)
                        .or().like(SysParameter::getParamValue, query != null ? query.getKeyword() : null)
                        .or().like(SysParameter::getParamComment, query != null ? query.getKeyword() : null))
                .orderByDesc(SysParameter::getSort)
                .orderByDesc(SysParameter::getId));
    }

    /**
     * 分页查询
     */
    public Page<SysParameter> page(SysParameterPageQuery query) {
        return page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysParameter>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                                .like(SysParameter::getParamKey, query.getKeyword())
                                .or().like(SysParameter::getParamValue, query.getKeyword())
                                .or().like(SysParameter::getParamComment, query.getKeyword()))
                        .orderByDesc(SysParameter::getSort)
                        .orderByDesc(SysParameter::getId));
    }
}
