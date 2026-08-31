package cn.utopiabin.cloud.platform.api.impl.tenant;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.api.tenant.TenantApi;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantListQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantPageQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.tenant.TenantVO;
import cn.utopiabin.cloud.platform.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 租户 API 实现
 * <p>
 * 委托 {@link TenantService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@org.springframework.validation.annotation.Validated
@DubboService
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户管理 Dubbo 服务实现")
public class TenantApiImpl implements TenantApi {

    private final TenantService tenantService;

    @Override
    public Long create(TenantCreateDTO dto) {
        return tenantService.create(dto);
    }

    @Override
    public void update(TenantUpdateDTO dto) {
        tenantService.update(dto);
    }

    @Override
    public void remove(Long id) {
        tenantService.remove(id);
    }

    @Override
    public void enable(Long id, Boolean available) {
        tenantService.enable(id, available);
    }

    @Override
    public TenantVO get(Long id) {
        return tenantService.get(id);
    }

    @Override
    public PageResult<TenantVO> page(TenantPageQuery query) {
        return tenantService.page(query);
    }

    @Override
    public List<TenantVO> list(TenantListQuery query) {
        return tenantService.list(query);
    }

    @Override
    public boolean existsByCode(String code) {
        return tenantService.existsByCode(code);
    }
}
