package cn.utopiabin.cloud.api.admin.controller.tenant;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.platform.api.tenant.TenantApi;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantListQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantPageQuery;
import cn.utopiabin.cloud.platform.model.dto.tenant.TenantUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.tenant.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 租户管理 REST 接口。 */
@Tag(name = "租户管理")
@Validated
@RestController
@RequestMapping("/tenants")
public class TenantController {

    @DubboReference
    private TenantApi tenantApi;

    @Operation(summary = "初始化已有租户的管理员")
    @PostMapping("/{id}/administrator")
    public RestResult<Void> provisionAdmin(@PathVariable Long id,
            @Valid @RequestBody cn.utopiabin.cloud.platform.model.dto.tenant.TenantAdminDTO dto) {
        tenantApi.provisionAdmin(id, dto);
        return RestResult.ok();
    }

    @Operation(summary = "新增租户")
    @PostMapping
    public RestResult<Long> create(@Valid @RequestBody TenantCreateDTO dto) {
        return RestResult.ok(tenantApi.create(dto));
    }

    @Operation(summary = "编辑租户")
    @PutMapping
    public RestResult<Void> update(@Valid @RequestBody TenantUpdateDTO dto) {
        tenantApi.update(dto);
        return RestResult.ok();
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public RestResult<Void> remove(@PathVariable Long id) {
        tenantApi.remove(id);
        return RestResult.ok();
    }

    @Operation(summary = "启用或禁用租户")
    @PatchMapping("/{id}/available")
    public RestResult<Void> enable(@PathVariable Long id, @RequestParam Boolean available) {
        tenantApi.enable(id, available);
        return RestResult.ok();
    }

    @Operation(summary = "查询租户详情")
    @GetMapping("/{id}")
    public RestResult<TenantVO> get(@PathVariable Long id) {
        return RestResult.ok(tenantApi.get(id));
    }

    @Operation(summary = "分页查询租户")
    @GetMapping
    public RestResult<PageResult<TenantVO>> page(@Valid @ModelAttribute TenantPageQuery query) {
        return RestResult.ok(tenantApi.page(query));
    }

    @Operation(summary = "列表查询租户")
    @GetMapping("/list")
    public RestResult<List<TenantVO>> list(@Valid @ModelAttribute TenantListQuery query) {
        return RestResult.ok(tenantApi.list(query));
    }

    @Operation(summary = "检查租户编码是否存在")
    @GetMapping("/exists")
    public RestResult<Boolean> existsByCode(@RequestParam String code) {
        return RestResult.ok(tenantApi.existsByCode(code));
    }
}
