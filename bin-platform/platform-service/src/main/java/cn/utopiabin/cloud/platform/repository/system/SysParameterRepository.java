package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.entity.system.SysParameter;
import cn.utopiabin.cloud.platform.mapper.system.SysParameterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统参数 Repository（直接供 ApiImpl 调用）
 *
 * @since 1.0
 */
@Repository
public class SysParameterRepository extends ServiceImpl<SysParameterMapper, SysParameter> {

    /**
     * 查单个，不存在则抛异常
     */
    public SysParameter getOrThrow(Long id) {
        SysParameter e = getById(id);
        if (e == null) {
            throw new BizException("参数配置不存在");
        }
        return e;
    }

    /**
     * 参数键是否已存在
     */
    public boolean keyExists(String key, long excludeId) {
        return count(new LambdaQueryWrapper<SysParameter>()
                .eq(SysParameter::getParamKey, key)
                .ne(SysParameter::getId, excludeId)) > 0;
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
     * 列表
     */
    public List<SysParameter> list(SysParameterListQuery query) {
        return list(new LambdaQueryWrapper<SysParameter>()
                .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                        .like(SysParameter::getParamKey, query.getKeyword())
                        .or().like(SysParameter::getParamValue, query.getKeyword())
                        .or().like(SysParameter::getParamComment, query.getKeyword()))
                .orderByDesc(SysParameter::getSort)
                .orderByDesc(SysParameter::getId));
    }

    /**
     * 分页
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
