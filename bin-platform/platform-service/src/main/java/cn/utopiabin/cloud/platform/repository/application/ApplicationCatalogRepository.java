package cn.utopiabin.cloud.platform.repository.application;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.entity.application.SysApplication;
import cn.utopiabin.cloud.platform.entity.application.SysApplicationRedirect;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationCatalogMapper;
import cn.utopiabin.cloud.platform.mapper.application.ApplicationRedirectMapper;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationDTO;
import cn.utopiabin.cloud.platform.model.dto.application.ApplicationQuery;
import cn.utopiabin.cloud.platform.model.dto.application.RedirectDTO;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

/** 应用产品目录数据仓库。 */
@Repository
@RequiredArgsConstructor
public class ApplicationCatalogRepository {
  private final ApplicationCatalogMapper mapper;
  private final ApplicationRedirectMapper redirectMapper;

  public SysApplication getApplication(long applicationId) {
    return require(mapper.selectById(applicationId));
  }

  public SysApplication lockClient(String clientId) {
    return mapper.selectOne(
        new LambdaQueryWrapper<SysApplication>()
            .eq(SysApplication::getServiceId, clientId)
            .eq(SysApplication::getStatus, "ENABLED")
            .eq(SysApplication::getSsoEnabled, true)
            .last("FOR UPDATE"));
  }

  public List<String> listRedirectUris(long applicationId) {
    return redirectMapper
        .selectList(
            new LambdaQueryWrapper<SysApplicationRedirect>()
                .select(SysApplicationRedirect::getRedirectUri)
                .eq(SysApplicationRedirect::getApplicationId, applicationId)
                .eq(SysApplicationRedirect::getAvailable, true))
        .stream()
        .map(SysApplicationRedirect::getRedirectUri)
        .toList();
  }

  public PageResult<ApplicationVO> page(ApplicationQuery query) {
    int page = Math.max(1, query.getPage());
    int size = Math.max(1, Math.min(100, query.getSize()));
    LambdaQueryWrapper<SysApplication> conditions = new LambdaQueryWrapper<SysApplication>();
    if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
      conditions.and(
          filter ->
              filter
                  .like(SysApplication::getCode, query.getKeyword())
                  .or()
                  .like(SysApplication::getName, query.getKeyword()));
    }
    conditions
        .eq(
            query.getStatus() != null && !query.getStatus().isEmpty(),
            SysApplication::getStatus,
            query.getStatus())
        .orderByAsc(SysApplication::getSort, SysApplication::getId);
    Page<SysApplication> result = mapper.selectPage(new Page<>(page, size), conditions);
    return PageResult.of(
        page, size, result.getTotal(), result.getRecords().stream().map(this::toView).toList());
  }

  public ApplicationVO get(long applicationId) {
    SysApplication application = mapper.selectById(applicationId);
    if (application == null) {
      return null;
    }
    ApplicationVO result = toView(application);
    result.setRedirectUris(listRedirects(applicationId));
    return result;
  }

  public List<RedirectDTO> listRedirects(long applicationId) {
    return redirectMapper
        .selectList(
            new LambdaQueryWrapper<SysApplicationRedirect>()
                .eq(SysApplicationRedirect::getApplicationId, applicationId)
                .orderByAsc(SysApplicationRedirect::getId))
        .stream()
        .map(
            redirect -> {
              RedirectDTO result = new RedirectDTO();
              BeanUtils.copyProperties(redirect, result);
              return result;
            })
        .toList();
  }

  public SysApplication lock(long applicationId) {
    return require(
        mapper.selectOne(
            new LambdaQueryWrapper<SysApplication>()
                .eq(SysApplication::getId, applicationId)
                .last("FOR UPDATE")));
  }

  public void requireExisting(long applicationId) {
    lock(applicationId);
  }

  public String lockService(long applicationId) {
    return lock(applicationId).getServiceId();
  }

  private ApplicationVO toView(SysApplication application) {
    ApplicationVO result = new ApplicationVO();
    BeanUtils.copyProperties(application, result);
    result.setClientConfigured(application.getClientSecretHash() != null);
    return result;
  }

  public int insert(long applicationId, ApplicationDTO dto, long operatorId) {
    SysApplication application = toEntity(applicationId, dto);
    application.setCreateUser(String.valueOf(operatorId));
    application.setModifyUser(String.valueOf(operatorId));
    return mapper.insert(application);
  }

  public int update(long applicationId, ApplicationDTO dto, long operatorId, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysApplication>()
            .set(SysApplication::getName, dto.getName())
            .set(SysApplication::getDescription, dto.getDescription())
            .set(SysApplication::getIconUrl, dto.getIconUrl())
            .set(SysApplication::getEntryUrl, dto.getEntryUrl())
            .set(SysApplication::getStatus, dto.getStatus())
            .set(SysApplication::getSsoEnabled, dto.isSsoEnabled())
            .set(SysApplication::getSort, dto.getSort())
            .set(SysApplication::getModifyUser, String.valueOf(operatorId))
            .setSql("version = version + 1")
            .eq(SysApplication::getId, applicationId)
            .eq(SysApplication::getVersion, version));
  }

  public void replaceRedirects(long applicationId, List<RedirectDTO> redirects) {
    redirectMapper.physicallyDeleteByApplication(applicationId);
    for (RedirectDTO redirect : redirects) {
      SysApplicationRedirect entity = new SysApplicationRedirect();
      entity.setId(IdWorker.getId());
      entity.setApplicationId(applicationId);
      entity.setEnvironment(redirect.getEnvironment());
      entity.setRedirectUri(redirect.getRedirectUri());
      entity.setLogoutUri(redirect.getLogoutUri());
      entity.setAvailable(redirect.isAvailable());
      redirectMapper.insert(entity);
    }
  }

  public int remove(long applicationId, int version) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysApplication>()
            .set(SysApplication::getIsDelete, 1)
            .set(SysApplication::getStatus, "OFFLINE")
            .setSql("version = version + 1")
            .eq(SysApplication::getId, applicationId)
            .eq(SysApplication::getVersion, version));
  }

  public int updateClientSecret(long applicationId, int version, String secretHash) {
    return mapper.update(
        null,
        new LambdaUpdateWrapper<SysApplication>()
            .set(SysApplication::getClientSecretHash, secretHash)
            .setSql("version = version + 1")
            .eq(SysApplication::getId, applicationId)
            .eq(SysApplication::getVersion, version));
  }

  private <T> T require(T value) {
    if (value == null) {
      throw new BizException(404, "应用不存在或已被并发删除");
    }
    return value;
  }

  private SysApplication toEntity(long applicationId, ApplicationDTO dto) {
    SysApplication application = new SysApplication();
    application.setId(applicationId);
    application.setCode(dto.getCode());
    application.setName(dto.getName());
    application.setDescription(dto.getDescription());
    application.setIconUrl(dto.getIconUrl());
    application.setEntryUrl(dto.getEntryUrl());
    application.setServiceId(dto.getServiceId());
    application.setStatus(dto.getStatus());
    application.setSsoEnabled(dto.isSsoEnabled());
    application.setSort(dto.getSort());
    return application;
  }
}
