package cn.utopiabin.cloud.platform.api.system;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterListQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterPageQuery;
import cn.utopiabin.cloud.platform.model.dto.system.SysParameterSaveDTO;
import cn.utopiabin.cloud.platform.model.vo.system.SysParameterVO;

import java.util.List;

/**
 * 系统参数 Dubbo API
 *
 * @author Bin
 * @version 1.0
 * @since 1.0
 */
public interface SysParameterApi {
    /**
     * 新增/编辑参数
     */
    void save(SysParameterSaveDTO dto);

    /**
     * 删除参数
     */
    void remove(Long id);

    /**
     * 分页查询参数
     */
    PageResult<SysParameterVO> page(SysParameterPageQuery query);

    /**
     * 列表查询参数
     */
    List<SysParameterVO> list(SysParameterListQuery query);

    /**
     * 按 key 获取参数值
     */
    String getValue(String key, String defaultValue);

    /**
     * 刷新缓存
     */
    void refreshCache();
}
