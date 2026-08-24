package cn.utopiabin.cloud.platform.api.tenant;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.tenant.*;
import cn.utopiabin.cloud.platform.model.vo.tenant.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 租户 Dubbo API
 * <p>
 * 多租户管理，含CRUD、启禁用等。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "租户管理", description = "多租户管理，含CRUD、启禁用")
public interface TenantApi {

    @Operation(summary = "新增租户", description = "租户编码全局唯一校验")
    Long create(@Parameter(description = "租户新增参数", required = true) TenantCreateDTO dto);

    @Operation(summary = "编辑租户", description = "按ID编辑租户信息")
    void update(@Parameter(description = "租户编辑参数", required = true) TenantUpdateDTO dto);

    @Operation(summary = "删除租户")
    void remove(@Parameter(description = "租户ID", required = true) Long id);

    @Operation(summary = "启用/禁用租户", description = "切换租户的启用状态")
    void enable(@Parameter(description = "租户ID", required = true) Long id,
                @Parameter(description = "是否启用", required = true) Boolean available);

    @Operation(summary = "查询租户详情")
    TenantVO get(@Parameter(description = "租户ID", required = true) Long id);

    @Operation(summary = "分页查询租户")
    PageResult<TenantVO> page(@Parameter(description = "分页查询条件", required = true) TenantPageQuery query);

    @Operation(summary = "列表查询租户")
    List<TenantVO> list(@Parameter(description = "列表查询条件") TenantListQuery query);

    @Operation(summary = "检查租户编码是否存在", description = "用于新增/编辑时的唯一性校验")
    boolean existsByCode(@Parameter(description = "租户编码", required = true) String code);
}
