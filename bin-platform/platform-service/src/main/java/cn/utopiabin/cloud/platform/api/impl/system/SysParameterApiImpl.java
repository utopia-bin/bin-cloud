package cn.utopiabin.cloud.platform.api.impl.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.api.system.SysParameterApi;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.system.SysParameterVO;
import cn.utopiabin.cloud.platform.service.system.SysParameterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 系统参数 API 实现
 * <p>
 * 委托 {@link SysParameterService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
@Tag(name = "系统参数", description = "系统参数 Dubbo 服务实现")
public class SysParameterApiImpl implements SysParameterApi {

    private final SysParameterService parameterService;

    @Override
    public void create(SysParameterCreateDTO dto) {
        parameterService.create(dto);
    }

    @Override
    public void update(SysParameterUpdateDTO dto) {
        parameterService.update(dto);
    }

    @Override
    public void remove(Long id) {
        parameterService.remove(id);
    }

    @Override
    public PageResult<SysParameterVO> page(SysParameterPageQuery query) {
        return parameterService.page(query);
    }

    @Override
    public List<SysParameterVO> list(SysParameterListQuery query) {
        return parameterService.list(query);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return parameterService.getValue(key, defaultValue);
    }

    @Override
    public void refreshCache() {
        parameterService.refreshCache();
    }
}
