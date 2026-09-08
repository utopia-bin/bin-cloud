package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.mapper.iam.SysRoleMapper;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRoleListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysRolePageQuery;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * 系统角色 Repository
 *
 * @since 1.0
 */
@Repository
public class SysRoleRepository extends BaseRepository<SysRoleMapper, SysRole> {

  @Override
  protected String getNotFoundMessage() {
    return "角色不存在";
  }

  /** 分页查询 */
  public Page<SysRole> page(SysRolePageQuery query) {
    return page(
        new Page<>(query.getPage(), query.getSize()),
        new LambdaQueryWrapper<SysRole>()
            .and(
                StrUtil.isNotBlank(query.getKeyword()),
                qw ->
                    qw.like(SysRole::getName, query.getKeyword())
                        .or()
                        .like(SysRole::getCode, query.getKeyword()))
            .eq(Objects.nonNull(query.getAvailable()), SysRole::getAvailable, query.getAvailable())
            .ge(Objects.nonNull(query.getStartTime()), SysRole::getGmtCreate, query.getStartTime())
            .le(Objects.nonNull(query.getEndTime()), SysRole::getGmtCreate, query.getEndTime())
            .orderByAsc(SysRole::getSort)
            .orderByDesc(SysRole::getId));
  }

  /** 列表查询 */
  public List<SysRole> list(SysRoleListQuery query) {
    return list(
        new LambdaQueryWrapper<SysRole>()
            .eq(Objects.nonNull(query.getAvailable()), SysRole::getAvailable, query.getAvailable())
            .orderByAsc(SysRole::getSort)
            .orderByDesc(SysRole::getId));
  }

  public List<SysRole> listByUserId(Long userId) {
    return baseMapper.selectRolesByUserId(userId);
  }

  public List<SysRoleVO> listViewsByUserId(Long userId) {
    return listByUserId(userId).stream().map(role -> role.copyTo(SysRoleVO.class)).toList();
  }

  public boolean consoleRoleExistsIncludingDeleted(Long tenantId, String code) {
    return !baseMapper.selectConsoleRoleIdsIncludingDeletedForUpdate(tenantId, code).isEmpty();
  }

  public long insertConsoleAdministrator(long tenantId, String name, String code) {
    SysRole role = new SysRole();
    role.setTenantId(tenantId);
    role.setApplicationId(1L);
    role.setTenantApplicationId(tenantId);
    role.setName(name);
    role.setCode(code);
    role.setDataScope(1);
    save(role);
    return role.getId();
  }
}
