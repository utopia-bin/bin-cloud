package cn.utopiabin.cloud.platform.api.system;

import cn.utopiabin.cloud.common.exception.BizException;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.system.SysParameterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 系统参数 Dubbo API
 * <p>
 * 提供系统参数的增删改查及缓存刷新能力。
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "系统参数", description = "系统参数管理，支持增删改查及缓存刷新")
public interface SysParameterApi {

    @Operation(summary = "新增参数", description = "参数键唯一性校验通过后新增")
    void create(@Parameter(description = "参数新增参数", required = true) SysParameterCreateDTO dto) throws BizException;

    @Operation(summary = "编辑参数", description = "按ID编辑参数，并刷新缓存")
    void update(@Parameter(description = "参数编辑参数", required = true) SysParameterUpdateDTO dto) throws BizException;

    @Operation(summary = "删除参数", description = "删除参数并清除对应缓存")
    void remove(@Parameter(description = "参数ID", required = true) Long id) throws BizException;

    @Operation(summary = "分页查询参数")
    PageResult<SysParameterVO> page(@Parameter(description = "分页查询条件", required = true) SysParameterPageQuery query) throws BizException;

    @Operation(summary = "列表查询参数")
    List<SysParameterVO> list(@Parameter(description = "列表查询条件") SysParameterListQuery query) throws BizException;

    @Operation(summary = "按key获取参数值", description = "从缓存获取，缓存不存在则查库并缓存")
    String getValue(
            @Parameter(description = "参数键", required = true) String key,
            @Parameter(description = "默认值，缓存和数据库均不存在时返回") String defaultValue) throws BizException;

    @Operation(summary = "刷新所有参数缓存", description = "清除参数缓存并重建")
    void refreshCache() throws BizException;
}
