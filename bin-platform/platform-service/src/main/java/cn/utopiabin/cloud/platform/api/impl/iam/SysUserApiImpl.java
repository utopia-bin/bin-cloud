package cn.utopiabin.cloud.platform.api.impl.iam;

import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.platform.api.iam.SysUserApi;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.*;
import cn.utopiabin.cloud.platform.service.iam.SysUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 系统用户 API 实现
 * <p>
 * 委托 {@link SysUserService} 处理业务逻辑。
 *
 * @since 1.0
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
@Tag(name = "系统用户", description = "系统用户 Dubbo 服务实现")
public class SysUserApiImpl implements SysUserApi {

    private final SysUserService userService;

    @Override
    public void create(SysUserCreateDTO dto) {
        userService.create(dto);
    }

    @Override
    public void update(SysUserUpdateDTO dto) {
        userService.update(dto);
    }

    @Override
    public void remove(Long id) {
        userService.remove(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        userService.batchDelete(dto);
    }

    @Override
    public void enable(Long id, Boolean available) {
        userService.enable(id, available);
    }

    @Override
    public SysUserVO get(Long id) {
        return userService.get(id);
    }

    @Override
    public PageResult<SysUserVO> page(SysUserPageQuery query) {
        return userService.page(query);
    }

    @Override
    public List<SysUserVO> list(SysUserListQuery query) {
        return userService.list(query);
    }

    @Override
    public void assignRoles(SysUserAssignRolesDTO dto) {
        userService.assignRoles(dto);
    }

    @Override
    public List<SysRoleVO> getRoles(Long userId) {
        return userService.getRoles(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userService.existsByUsername(username);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        userService.resetPassword(userId, newPassword);
    }
}
