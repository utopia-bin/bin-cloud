package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.common.annotations.DistributedLock;
import cn.utopiabin.cloud.platform.annotation.OperateLog;
import cn.utopiabin.cloud.platform.annotation.OperateType;
import cn.utopiabin.cloud.common.annotations.RepeatSubmit;
import cn.utopiabin.cloud.platform.constant.PlatformConstants;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.annotation.RequirePermission;
import cn.utopiabin.cloud.platform.entity.iam.SysPermission;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.mapper.iam.SysPermissionMapper;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.SysPermissionVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.repository.iam.SysPermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRolePermissionRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRoleRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 系统角色服务
 * <p>
 * 生产级能力:
 * <ul>
 *   <li>内置超级管理员角色保护 (不可删除/禁用/修改编码)</li>
 *   <li>防重复提交 (创建)</li>
 *   <li>分布式锁 (分配菜单，防并发竞态)</li>
 *   <li>缓存失效延迟到事务提交后执行 (避免脏缓存)</li>
 *   <li>操作日志审计</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleRepository roleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysPermissionMapper permissionMapper;
    private final PermissionService permissionService;

    @RepeatSubmit
    @OperateLog(module = "角色管理", action = "新增角色", type = OperateType.CREATE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:role:create")
    public Long create(SysRoleCreateDTO dto) {
        validateDataScope(dto.getDataScope());
        var code = dto.getCode() == null ? "" : dto.getCode().trim();
        if (StrUtil.isBlank(code)) {
            throw new BizException(PlatformErrorCode.BAD_REQUEST.getCode(),
                    "角色编码不能为空");
        }
        if (roleRepository.countByField(SysRole::getCode, code, null) > 0) {
            throw new BizException(PlatformErrorCode.ROLE_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.ROLE_CODE_DUPLICATE.getMsg());
        }

        var role = dto.copyTo(SysRole.class);
        role.setTenantApplicationId(Long.valueOf(cn.utopiabin.cloud.common.context.UserContextHolder.getTenantId()));
        role.setName(dto.getName().trim());
        role.setCode(code);
        role.setDataScope(Optional.ofNullable(dto.getDataScope()).orElse(1));
        role.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        role.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        role.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        roleRepository.save(role);
        return role.getId();
    }

    @RepeatSubmit
    @OperateLog(module = "角色管理", action = "编辑角色", type = OperateType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:role:update")
    public void update(SysRoleUpdateDTO dto) {
        validateDataScope(dto.getDataScope());
        var role = roleRepository.getOrThrow(dto.getId());
        if (!java.util.Objects.equals(role.getVersion(), dto.getExpectedVersion())) {
            throw new BizException(PlatformErrorCode.ROLE_VERSION_CONFLICT.getCode(),
                    PlatformErrorCode.ROLE_VERSION_CONFLICT.getMsg());
        }
        var code = dto.getCode() == null ? "" : dto.getCode().trim();
        if (StrUtil.isBlank(code)) {
            throw new BizException(PlatformErrorCode.BAD_REQUEST.getCode(),
                    "角色编码不能为空");
        }
        if (roleRepository.countByField(SysRole::getCode, code, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.ROLE_CODE_DUPLICATE.getCode(),
                    PlatformErrorCode.ROLE_CODE_DUPLICATE.getMsg());
        }

        // 内置角色保护: 超级管理员角色不可修改编码
        if (isBuiltInSuperAdmin(role) && !PlatformConstants.SUPER_ADMIN_ROLE_CODE.equals(code)) {
            throw new BizException(PlatformErrorCode.BUILT_IN_PROTECTED.getCode(),
                    "内置超级管理员角色不允许修改编码");
        }

        var newAvailable = Optional.ofNullable(dto.getAvailable()).orElse(role.getAvailable());
        // 禁用保护: 超级管理员角色不可禁用 (与 enable 口径一致，防止经 update 绕过)
        if (!Boolean.TRUE.equals(newAvailable) && Boolean.TRUE.equals(role.getAvailable())) {
            assertNotBuiltInSuperAdmin(role);
        }

        role.setName(dto.getName().trim());
        role.setCode(code);
        role.setDataScope(Optional.ofNullable(dto.getDataScope()).orElse(role.getDataScope()));
        role.setSort(Optional.ofNullable(dto.getSort()).orElse(role.getSort()));
        role.setAvailable(newAvailable);
        role.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        if (!roleRepository.updateById(role)) {
            throw new BizException(PlatformErrorCode.ROLE_VERSION_CONFLICT.getCode(),
                    PlatformErrorCode.ROLE_VERSION_CONFLICT.getMsg());
        }

        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @OperateLog(module = "角色管理", action = "删除角色", type = OperateType.DELETE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:role:delete")
    public void remove(Long id) {
        var role = roleRepository.getOrThrow(id);
        // 内置角色保护: 超级管理员角色不可删除
        assertNotBuiltInSuperAdmin(role);

        rolePermissionRepository.removeByRoleId(id);
        userRoleRepository.removeByRoleId(id);
        roleRepository.removeById(id);
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @OperateLog(module = "角色管理", action = "批量删除角色", type = OperateType.DELETE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:role:delete")
    public void batchDelete(BatchDeleteDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            return;
        }
        // 存在性校验 + 内置角色保护
        var roles = roleRepository.listByIds(dto.getIds());
        if (roles.size() != dto.getIds().size()) {
            throw new BizException(PlatformErrorCode.ROLE_NOT_FOUND.getCode(),
                    PlatformErrorCode.ROLE_NOT_FOUND.getMsg());
        }
        roles.forEach(this::assertNotBuiltInSuperAdmin);

        for (Long id : dto.getIds()) {
            rolePermissionRepository.removeByRoleId(id);
            userRoleRepository.removeByRoleId(id);
        }
        roleRepository.removeByIds(dto.getIds());
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @OperateLog(module = "角色管理", action = "启用/禁用角色", type = OperateType.ENABLE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:role:update")
    public void enable(Long id, Boolean available) {
        var role = roleRepository.getOrThrow(id);
        // 仅禁用时需要保护: 超级管理员角色不可禁用
        if (!Boolean.TRUE.equals(available)) {
            assertNotBuiltInSuperAdmin(role);
        }
        role.setAvailable(available);
        roleRepository.updateById(role);
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @RequirePermission("platform:role:read")
    public SysRoleVO get(Long id) {
        return roleRepository.getOrThrow(id).copyTo(SysRoleVO.class);
    }

    @RequirePermission("platform:role:read")
    public PageResult<SysRoleVO> page(SysRolePageQuery query) {
        Page<SysRole> page = roleRepository.page(query);
        var records = page.getRecords().stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @RequirePermission("platform:role:read")
    public List<SysRoleVO> list(SysRoleListQuery query) {
        return roleRepository.list(query).stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();
    }

    @DistributedLock(key = "'role:assignPermissions:' + #dto.roleId")
    @OperateLog(module = "角色管理", action = "分配权限", type = OperateType.ASSIGN)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:role:assign-permission")
    public void assignPermissions(SysRoleAssignPermissionsDTO dto) {
        var role = roleRepository.getOrThrow(dto.getRoleId());
        if (!java.util.Objects.equals(role.getVersion(), dto.getExpectedVersion())) {
            throw new BizException(PlatformErrorCode.ROLE_VERSION_CONFLICT.getCode(),
                    PlatformErrorCode.ROLE_VERSION_CONFLICT.getMsg());
        }

        var requestedIds = dto.getPermissionIds().stream().distinct().toList();
        var permissions = requestedIds.isEmpty()
                ? List.<SysPermission>of()
                : permissionRepository.listByIds(requestedIds);
        if (permissions.size() != requestedIds.size()
                || permissions.stream().anyMatch(p -> !Boolean.TRUE.equals(p.getAvailable()))) {
            throw new BizException(PlatformErrorCode.PERMISSION_NOT_FOUND.getCode(),
                    "权限不存在或已被禁用");
        }

        Long operator = Long.valueOf(cn.utopiabin.cloud.common.context.UserContextHolder.getUserId());
        if (permissions.stream().anyMatch(p -> !permissionService.hasPermission(operator, p.getCode()))) {
            throw new BizException(403, "不能分配超出自身授权范围的权限");
        }
        rolePermissionRepository.replace(role.getTenantId(), role.getId(), requestedIds);
        if (!roleRepository.updateById(role)) {
            throw new BizException(PlatformErrorCode.ROLE_VERSION_CONFLICT.getCode(),
                    PlatformErrorCode.ROLE_VERSION_CONFLICT.getMsg());
        }
        TransactionAfterCommitExecutor.afterCommit(permissionService::evictAllUserPermissions);
    }

    @RequirePermission("platform:role:read")
    public List<SysPermissionVO> getPermissions(Long roleId) {
        roleRepository.getOrThrow(roleId);
        return permissionMapper.selectByRoleIds(List.of(roleId)).stream()
                .map(p -> p.copyTo(SysPermissionVO.class))
                .toList();
    }

    @RequirePermission("platform:role:read")
    public boolean existsByCode(String code) {
        return roleRepository.exists(SysRole::getCode, code);
    }

    private void validateDataScope(Integer scope) {
        if (scope != null && scope != 1 && scope != 4) {
            throw new BizException(400, "当前未启用部门组织，只支持全部或仅本人数据范围");
        }
    }

    // ==================== 保护校验 ====================

    /**
     * 判断是否内置超级管理员角色
     */
    private boolean isBuiltInSuperAdmin(SysRole role) {
        return PlatformConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode());
    }

    /**
     * 内置角色保护: 超级管理员角色不可删除/禁用
     */
    private void assertNotBuiltInSuperAdmin(SysRole role) {
        if (isBuiltInSuperAdmin(role)) {
            throw new BizException(PlatformErrorCode.BUILT_IN_PROTECTED.getCode(),
                    "内置超级管理员角色不允许此操作");
        }
    }
}
