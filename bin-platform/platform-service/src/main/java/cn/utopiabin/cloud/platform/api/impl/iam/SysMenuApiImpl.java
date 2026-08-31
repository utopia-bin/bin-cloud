package cn.utopiabin.cloud.platform.api.impl.iam;

import cn.utopiabin.cloud.platform.api.iam.SysMenuApi;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuCreateDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuListQuery;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuUpdateDTO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuTreeVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysMenuVO;
import cn.utopiabin.cloud.platform.service.iam.SysMenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 系统菜单 API 实现
 * <p>
 * 委托 {@link SysMenuService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@org.springframework.validation.annotation.Validated
@DubboService
@RequiredArgsConstructor
@Tag(name = "系统菜单", description = "系统菜单 Dubbo 服务实现")
public class SysMenuApiImpl implements SysMenuApi {

    private final SysMenuService menuService;

    @Override
    public Long create(SysMenuCreateDTO dto) {
        return menuService.create(dto);
    }

    @Override
    public void update(SysMenuUpdateDTO dto) {
        menuService.update(dto);
    }

    @Override
    public void remove(Long id) {
        menuService.remove(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        menuService.batchDelete(dto);
    }

    @Override
    public SysMenuVO get(Long id) {
        return menuService.get(id);
    }

    @Override
    public List<SysMenuVO> list(SysMenuListQuery query) {
        return menuService.list(query);
    }

    @Override
    public List<SysMenuTreeVO> tree(SysMenuListQuery query) {
        return menuService.tree(query);
    }

    @Override
    public List<SysMenuVO> listByPermissionCodes(List<String> permissionCodes) {
        return menuService.listByPermissionCodes(permissionCodes);
    }
}
