package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationPermissionMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationResourceDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationResourceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/** 应用产品权限资源数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationPermissionRepository extends ApplicationRepositorySupport {

  private final ApplicationPermissionMapper mapper;

  public List<ApplicationResourceVO> listPermissions(long applicationId) {
    return mapper
        .selectList(
            new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getApplicationId, applicationId)
                .orderByAsc(SysPermission::getSort, SysPermission::getId))
        .stream()
        .map(this::toView)
        .toList();
  }

  public SysPermission getPermission(long id, long applicationId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getId, id)
                .eq(SysPermission::getApplicationId, applicationId)));
  }

  public void requirePermission(long permissionId, long applicationId) {
    require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysPermission>()
                .select(SysPermission::getId)
                .eq(SysPermission::getId, permissionId)
                .eq(SysPermission::getApplicationId, applicationId)
                .eq(SysPermission::getAvailable, true)));
  }

  public void requirePermissionCode(String code, long applicationId) {
    require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysPermission>()
                .select(SysPermission::getId)
                .eq(SysPermission::getCode, code)
                .eq(SysPermission::getApplicationId, applicationId)));
  }

  public List<Long> listAvailableIds(long applicationId) {
    return mapper
        .selectList(
            new LambdaQueryWrapper<SysPermission>()
                .select(SysPermission::getId)
                .eq(SysPermission::getApplicationId, applicationId)
                .eq(SysPermission::getAvailable, true))
        .stream()
        .map(SysPermission::getId)
        .toList();
  }

  public List<String> listPermissionCodes(
      long applicationId, long tenantId, long instanceId, long userId) {
    return mapper.selectUserApplicationPermissionCodes(applicationId, tenantId, instanceId, userId);
  }

  public void insertPermission(long id, ApplicationResourceDTO dto) {
    SysPermission permission = new SysPermission();
    permission.setId(id);
    permission.setApplicationId(dto.getApplicationId());
    permission.setName(dto.getName());
    permission.setCode(dto.getCode());
    permission.setDescription(dto.getDescription());
    permission.setAvailable(dto.isAvailable());
    permission.setSort(dto.getSort());
    mapper.insert(permission);
  }

  public int updatePermission(long id, ApplicationResourceDTO dto, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysPermission>()
            .set(SysPermission::getName, dto.getName())
            .set(SysPermission::getDescription, dto.getDescription())
            .set(SysPermission::getAvailable, dto.isAvailable())
            .set(SysPermission::getSort, dto.getSort())
            .setSql("version = version + 1")
            .eq(SysPermission::getId, id)
            .eq(SysPermission::getApplicationId, dto.getApplicationId())
            .eq(SysPermission::getVersion, version));
  }

  public int removePermission(long id, long applicationId, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysPermission>()
            .set(SysPermission::getIsDelete, 1)
            .setSql("version = version + 1")
            .eq(SysPermission::getId, id)
            .eq(SysPermission::getApplicationId, applicationId)
            .eq(SysPermission::getVersion, version));
  }

  private ApplicationResourceVO toView(SysPermission permission) {
    ApplicationResourceVO result = new ApplicationResourceVO();
    BeanUtils.copyProperties(permission, result);
    return result;
  }
}
