package cn.utopiabin.cloud.platform.service.tenant;

import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.entity.tenant.Tenant;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantListQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantPageQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.tenant.TenantVO;
import cn.utopiabin.cloud.platform.repository.tenant.TenantRepository;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 租户服务
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:tenant:create")
    @OperateLog(module = "租户管理", action = "新增租户", type = OperateType.CREATE, maskParams = true)
    public Long create(TenantCreateDTO dto) {
        var code = dto.getCode().trim();
        if (tenantRepository.countByField(Tenant::getCode, code, null) > 0) {
            throw new BizException(PlatformErrorCode.TENANT_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.TENANT_CODE_DUPLICATE.getMsg());
        }

        var tenant = dto.copyTo(Tenant.class);
        tenant.setName(dto.getName().trim());
        tenant.setCode(code);
        tenant.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        tenant.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        tenant.setContactName(StrUtil.defaultIfBlank(dto.getContactName(), ""));
        tenant.setContactPhone(StrUtil.defaultIfBlank(dto.getContactPhone(), ""));
        tenant.setContactEmail(StrUtil.defaultIfBlank(dto.getContactEmail(), ""));
        tenant.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        tenantRepository.save(tenant);
        provisioningService.provision(tenant.getId(), dto);
        return tenant.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:tenant:update")
    @OperateLog(module = "租户管理", action = "初始化租户管理员", type = OperateType.CREATE, maskParams = true)
    public void provisionAdmin(Long id, cn.utopiabin.cloud.platform.model.dto.tenant.TenantAdminDTO dto) {
        tenantRepository.getOrThrow(id);
        provisioningService.provision(id, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:tenant:update")
    @OperateLog(module = "租户管理", action = "修改租户", type = OperateType.UPDATE, maskParams = true)
    public void update(TenantUpdateDTO dto) {
        var tenant = tenantRepository.getOrThrow(dto.getId());
        if (!java.util.Objects.equals(tenant.getVersion(), dto.getExpectedVersion())) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(), "租户已被修改，请刷新后重试");
        }
        var code = dto.getCode().trim();
        if (tenantRepository.countByField(Tenant::getCode, code, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.TENANT_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.TENANT_CODE_DUPLICATE.getMsg());
        }

        tenant.setName(dto.getName().trim());
        tenant.setCode(code);
        tenant.setContactName(StrUtil.defaultIfBlank(dto.getContactName(), ""));
        tenant.setContactPhone(StrUtil.defaultIfBlank(dto.getContactPhone(), ""));
        tenant.setContactEmail(StrUtil.defaultIfBlank(dto.getContactEmail(), ""));
        tenant.setExpireTime(dto.getExpireTime());
        tenant.setSort(Optional.ofNullable(dto.getSort()).orElse(tenant.getSort()));
        tenant.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(tenant.getAvailable()));
        tenant.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        if (!tenantRepository.updateById(tenant)) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(), "租户已被修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:tenant:delete")
    @OperateLog(module = "租户管理", action = "删除租户", type = OperateType.DELETE, maskParams = true)
    public void remove(Long id) {
        tenantRepository.getOrThrow(id);
        tenantRepository.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:tenant:update")
    @OperateLog(module = "租户管理", action = "修改状态租户", type = OperateType.ENABLE, maskParams = true)
    public void enable(Long id, Boolean available) {
        var tenant = tenantRepository.getOrThrow(id);
        tenant.setAvailable(available);
        tenantRepository.updateById(tenant);
    }

    @RequirePermission("platform:tenant:read")
    public TenantVO get(Long id) {
        return tenantRepository.getOrThrow(id).copyTo(TenantVO.class);
    }

    @RequirePermission("platform:tenant:read")
    public PageResult<TenantVO> page(TenantPageQuery query) {
        Page<Tenant> page = tenantRepository.page(query);
        var records = page.getRecords().stream()
                .map(t -> t.copyTo(TenantVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @RequirePermission("platform:tenant:read")
    public List<TenantVO> list(TenantListQuery query) {
        return tenantRepository.list(query).stream()
                .map(t -> t.copyTo(TenantVO.class))
                .toList();
    }

    @RequirePermission("platform:tenant:read")
    public boolean existsByCode(String code) {
        return tenantRepository.exists(Tenant::getCode, code);
    }
}
