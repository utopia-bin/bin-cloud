package cn.utopiabin.cloud.platform.repository.tenant;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.mapper.tenant.TenantMapper;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantListQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantPageQuery;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 租户 Repository
 *
 * @since 1.0
 */
@Repository
public class TenantRepository extends BaseRepository<TenantMapper, Tenant> {

    @Override
    protected String getNotFoundMessage() {
        return "租户不存在";
    }

    /**
     * 分页查询
     */
    public Page<Tenant> page(TenantPageQuery query) {
        return page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<Tenant>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), qw -> qw
                                .like(Tenant::getName, query.getKeyword())
                                .or()
                                .like(Tenant::getCode, query.getKeyword()))
                        .eq(Objects.nonNull(query.getAvailable()), Tenant::getAvailable, query.getAvailable())
                        .ge(Objects.nonNull(query.getStartTime()), Tenant::getGmtCreate, query.getStartTime())
                        .le(Objects.nonNull(query.getEndTime()), Tenant::getGmtCreate, query.getEndTime())
                        .orderByAsc(Tenant::getSort)
                        .orderByDesc(Tenant::getId));
    }

    /**
     * 列表查询
     */
    public List<Tenant> list(TenantListQuery query) {
        return list(new LambdaQueryWrapper<Tenant>()
                .eq(Objects.nonNull(query.getAvailable()), Tenant::getAvailable, query.getAvailable())
                .orderByAsc(Tenant::getSort)
                .orderByDesc(Tenant::getId));
    }
}
