package cn.utopiabin.cloud.platform.api.impl.iam;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.api.iam.SysRoleApi;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.*;
import cn.utopiabin.cloud.platform.service.iam.SysRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 系统角色 API 实现
 * <p>
 * 委托 {@link SysRoleService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
@Tag(name = "系统角色", description = "系统角色 Dubbo 服务实现")
public class SysRoleApiImpl implements SysRoleApi {

    private final SysRoleService roleService;

    @Override
    public void create(SysRoleCreateDTO dto) {
        roleService.create(dto);
    }

    @Override
    public void update(SysRoleUpdateDTO dto) {
        roleService.update(dto);
    }

    @Override
    public void remove(Long id) {
        roleService.remove(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        roleService.batchDelete(dto);
    }

    @Override
    public void enable(Long id, Boolean available) {
        roleService.enable(id, available);
    }

    @Override
    public SysRoleVO get(Long id) {
        return roleService.get(id);
    }

    @Override
    public PageResult<SysRoleVO> page(SysRolePageQuery query) {
        return roleService.page(query);
    }

    @Override
    public List<SysRoleVO> list(SysRoleListQuery query) {
        return roleService.list(query);
    }

    @Override
    public void assignMenus(SysRoleAssignMenusDTO dto) {
        roleService.assignMenus(dto);
    }

    @Override
    public List<SysMenuVO> getMenus(Long roleId) {
        return roleService.getMenus(roleId);
    }

    @Override
    public boolean existsByCode(String code) {
        return roleService.existsByCode(code);
    }
}
