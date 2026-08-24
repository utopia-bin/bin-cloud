package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.mapper.iam.SysUserMapper;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysUserPageQuery;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 系统用户 Repository
 *
 * @since 1.0
 */
@Repository
public class SysUserRepository extends BaseRepository<SysUserMapper, SysUser> {

    @Override
    protected String getNotFoundMessage() {
        return "用户不存在";
    }

    /** 根据租户和用户名查询用户。 */
    public SysUser getByTenantIdAndUsername(Long tenantId, String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, username));
    }

    /**
     * 分页查询
     */
    public Page<SysUser> page(SysUserPageQuery query) {
        return page(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysUser>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), qw -> qw
                                .like(SysUser::getUsername, query.getKeyword())
                                .or()
                                .like(SysUser::getRealName, query.getKeyword())
                                .or()
                                .like(SysUser::getPhone, query.getKeyword()))
                        .eq(Objects.nonNull(query.getAvailable()), SysUser::getAvailable, query.getAvailable())
                        .ge(Objects.nonNull(query.getStartTime()), SysUser::getGmtCreate, query.getStartTime())
                        .le(Objects.nonNull(query.getEndTime()), SysUser::getGmtCreate, query.getEndTime())
                        .orderByAsc(SysUser::getSort)
                        .orderByDesc(SysUser::getId));
    }

    /**
     * 列表查询
     */
    public List<SysUser> list(SysUserListQuery query) {
        return list(new LambdaQueryWrapper<SysUser>()
                .eq(Objects.nonNull(query.getAvailable()), SysUser::getAvailable, query.getAvailable())
                .orderByAsc(SysUser::getSort)
                .orderByDesc(SysUser::getId));
    }
}
