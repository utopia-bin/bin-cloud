package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.context.UserContextHolder;
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
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.mapper.iam.SysUserMapper;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRoleRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysRoleRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import cn.utopiabin.cloud.platform.util.PasswordValidator;
import cn.utopiabin.cloud.platform.util.TransactionAfterCommitExecutor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 系统用户服务
 * <p>
 * 生产级能力:
 * <ul>
 *   <li>内置管理员账号保护 (不可删除/禁用)</li>
 *   <li>自我保护 (不可删除/禁用当前登录账号，防止锁死系统)</li>
 *   <li>密码强度校验 (创建/编辑/重置密码)</li>
 *   <li>防重复提交 (创建/编辑)</li>
 *   <li>分布式锁 (分配角色/重置密码，防并发竞态)</li>
 *   <li>缓存失效延迟到事务提交后执行 (避免脏缓存)</li>
 *   <li>操作日志审计</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final PasswordValidator passwordValidator;

    @RepeatSubmit
    @OperateLog(module = "用户管理", action = "新增用户", type = OperateType.CREATE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:create")
    public Long create(SysUserCreateDTO dto) {
        var username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (StrUtil.isBlank(username) || StrUtil.isBlank(dto.getPassword())) {
            throw new BizException(PlatformErrorCode.BAD_REQUEST.getCode(),
                    "用户名与密码不能为空");
        }
        if (userRepository.countByField(SysUser::getUsername, username, null) > 0) {
            throw new BizException(PlatformErrorCode.USER_DUPLICATE.getCode(),
                    PlatformErrorCode.USER_DUPLICATE.getMsg());
        }
        var phone = StrUtil.defaultIfBlank(dto.getPhone(), "").trim();
        if (StrUtil.isNotBlank(phone)
                && userRepository.countByField(SysUser::getPhone, phone, null) > 0) {
            throw new BizException(PlatformErrorCode.PHONE_DUPLICATE.getCode(),
                    PlatformErrorCode.PHONE_DUPLICATE.getMsg());
        }

        // 密码强度校验
        passwordValidator.validate(dto.getPassword());

        var user = dto.copyTo(SysUser.class);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        user.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        user.setGender(Optional.ofNullable(dto.getGender()).orElse(0));
        user.setRealName(StrUtil.defaultIfBlank(dto.getRealName(), ""));
        user.setPhone(phone);
        user.setEmail(StrUtil.defaultIfBlank(dto.getEmail(), ""));
        user.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        userRepository.save(user);
        return user.getId();
    }

    @RepeatSubmit
    @OperateLog(module = "用户管理", action = "编辑用户", type = OperateType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:update")
    public void update(SysUserUpdateDTO dto) {
        var user = userRepository.getOrThrow(dto.getId());
        if (!Objects.equals(user.getVersion(), dto.getExpectedVersion())) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(), "用户已被修改，请刷新后重试");
        }
        var username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (StrUtil.isBlank(username)) {
            throw new BizException(PlatformErrorCode.BAD_REQUEST.getCode(),
                    "用户名不能为空");
        }
        if (userRepository.countByField(SysUser::getUsername, username, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.USER_DUPLICATE.getCode(),
                    PlatformErrorCode.USER_DUPLICATE.getMsg());
        }
        var phone = StrUtil.defaultIfBlank(dto.getPhone(), "").trim();
        if (StrUtil.isNotBlank(phone)
                && userRepository.countByField(SysUser::getPhone, phone, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.PHONE_DUPLICATE.getCode(),
                    PlatformErrorCode.PHONE_DUPLICATE.getMsg());
        }

        // 内置管理员保护: 不允许改名 (改名会使内置保护失效)
        if (isBuiltInAdmin(user) && !user.getUsername().equals(username)) {
            throw new BizException(PlatformErrorCode.BUILT_IN_PROTECTED.getCode(),
                    "内置管理员账号不允许修改用户名");
        }

        var newAvailable = Optional.ofNullable(dto.getAvailable()).orElse(user.getAvailable());
        // 禁用保护: 不可禁用自己/内置管理员 (与 enable 口径一致)
        if (!Boolean.TRUE.equals(newAvailable) && Boolean.TRUE.equals(user.getAvailable())) {
            assertNotSelf(dto.getId());
            assertNotBuiltInAdmin(user);
        }

        // 编辑时携带新密码才校验强度并更新
        if (StrUtil.isNotBlank(dto.getPassword())) {
            passwordValidator.validate(dto.getPassword());
            user.setCredentialVersion(java.util.Optional.ofNullable(user.getCredentialVersion()).orElse(0) + 1);
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        user.setUsername(username);
        user.setRealName(StrUtil.defaultIfBlank(dto.getRealName(), ""));
        user.setPhone(phone);
        user.setEmail(StrUtil.defaultIfBlank(dto.getEmail(), ""));
        user.setGender(Optional.ofNullable(dto.getGender()).orElse(user.getGender()));
        user.setSort(Optional.ofNullable(dto.getSort()).orElse(user.getSort()));
        user.setAvailable(newAvailable);
        user.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        if (!userRepository.updateById(user)) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(), "用户已被修改，请刷新后重试");
        }

        // 缓存失效延迟到事务提交后执行
        TransactionAfterCommitExecutor.afterCommit(() ->
                permissionService.evictUserPermission(dto.getId()));
    }

    @OperateLog(module = "用户管理", action = "删除用户", type = OperateType.DELETE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:delete")
    public void remove(Long id) {
        var user = userRepository.getOrThrow(id);
        // 自我保护 + 内置管理员保护
        assertNotSelf(id);
        assertNotBuiltInAdmin(user);

        userRoleRepository.removeByUserId(id);
        userRepository.removeById(id);
        TransactionAfterCommitExecutor.afterCommit(() ->
                permissionService.evictUserPermission(id));
    }

    @OperateLog(module = "用户管理", action = "批量删除用户", type = OperateType.DELETE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:delete")
    public void batchDelete(BatchDeleteDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            return;
        }
        // 存在性校验 + 保护校验
        var users = userRepository.listByIds(dto.getIds());
        if (users.size() != dto.getIds().size()) {
            throw new BizException(PlatformErrorCode.USER_NOT_FOUND.getCode(),
                    PlatformErrorCode.USER_NOT_FOUND.getMsg());
        }
        users.forEach(this::assertNotBuiltInAdmin);
        dto.getIds().forEach(this::assertNotSelf);

        for (Long id : dto.getIds()) {
            userRoleRepository.removeByUserId(id);
        }
        userRepository.removeByIds(dto.getIds());
        TransactionAfterCommitExecutor.afterCommit(() ->
                dto.getIds().forEach(permissionService::evictUserPermission));
    }

    @OperateLog(module = "用户管理", action = "启用/禁用用户", type = OperateType.ENABLE)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:update")
    public void enable(Long id, Boolean available) {
        var user = userRepository.getOrThrow(id);
        // 仅禁用时需要保护: 防止禁用自己/内置管理员导致系统锁死
        if (!Boolean.TRUE.equals(available)) {
            assertNotSelf(id);
            assertNotBuiltInAdmin(user);
        }
        user.setAvailable(available);
        userRepository.updateById(user);
        TransactionAfterCommitExecutor.afterCommit(() ->
                permissionService.evictUserPermission(id));
    }

    @RequirePermission("platform:user:read")
    public SysUserVO get(Long id) {
        return userRepository.getOrThrow(id).copyTo(SysUserVO.class);
    }

    @RequirePermission("platform:user:read")
    public PageResult<SysUserVO> page(SysUserPageQuery query) {
        Page<SysUser> page = userRepository.page(query);
        var records = page.getRecords().stream()
                .map(u -> u.copyTo(SysUserVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @RequirePermission("platform:user:read")
    public List<SysUserVO> list(SysUserListQuery query) {
        return userRepository.list(query).stream()
                .map(u -> u.copyTo(SysUserVO.class))
                .toList();
    }

    @DistributedLock(key = "'user:assignRoles:' + #dto.userId")
    @OperateLog(module = "用户管理", action = "分配角色", type = OperateType.ASSIGN)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:assign-role")
    public void assignRoles(SysUserAssignRolesDTO dto) {
        var user = userRepository.getOrThrow(dto.getUserId());
        if (!Objects.equals(user.getVersion(), dto.getExpectedVersion())) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(),
                    "用户已被其他操作修改，请刷新后重试");
        }
        var roleIds = dto.getRoleIds().stream().distinct().toList();
        var roles = roleIds.isEmpty() ? List.<SysRole>of() : roleRepository.listByIds(roleIds);
        if (roles.size() != roleIds.size()
                || roles.stream().anyMatch(role -> !Boolean.TRUE.equals(role.getAvailable()))) {
            throw new BizException(PlatformErrorCode.TENANT_RELATION_VIOLATION.getCode(),
                    "角色不存在、已禁用或不属于当前租户");
        }
        userRoleRepository.assignRoles(user.getTenantId(), dto.getUserId(), roleIds);
        if (!userRepository.updateById(user)) {
            throw new BizException(PlatformErrorCode.CONFLICT.getCode(),
                    "用户已被其他操作修改，请刷新后重试");
        }
        TransactionAfterCommitExecutor.afterCommit(() ->
                permissionService.evictUserPermission(dto.getUserId()));
    }

    @RequirePermission("platform:user:read")
    public List<SysRoleVO> getRoles(Long userId) {
        List<SysRole> roles = userMapper.selectRolesByUserId(userId);
        return roles.stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();
    }

    @RequirePermission("platform:user:read")
    public boolean existsByUsername(String username) {
        return userRepository.exists(SysUser::getUsername, username);
    }

    @DistributedLock(key = "'user:resetPassword:' + #userId")
    @OperateLog(module = "用户管理", action = "重置密码", type = OperateType.AUTH, maskParams = true)
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("platform:user:reset-password")
    public void resetPassword(Long userId, String newPassword) {
        var user = userRepository.getOrThrow(userId);
        passwordValidator.validate(newPassword);
        user.setCredentialVersion(java.util.Optional.ofNullable(user.getCredentialVersion()).orElse(0) + 1);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(user);
        log.info("重置用户密码: userId={}, operator={}", userId, UserContextHolder.getUsername());
    }

    // ==================== 保护校验 ====================

    /**
     * 自我保护: 不可删除/禁用当前登录账号
     */
    private void assertNotSelf(Long targetUserId) {
        String currentId = UserContextHolder.getUserId();
        if (currentId != null && Objects.equals(currentId, String.valueOf(targetUserId))) {
            throw new BizException(PlatformErrorCode.CANNOT_OPERATE_SELF.getCode(),
                    PlatformErrorCode.CANNOT_OPERATE_SELF.getMsg());
        }
    }

    /**
     * 判断是否内置管理员账号
     */
    private boolean isBuiltInAdmin(SysUser user) {
        return PlatformConstants.BUILT_IN_ADMIN_USERNAME.equals(user.getUsername());
    }

    /**
     * 内置管理员保护: admin 账号不可删除/禁用
     */
    private void assertNotBuiltInAdmin(SysUser user) {
        if (isBuiltInAdmin(user)) {
            throw new BizException(PlatformErrorCode.BUILT_IN_PROTECTED.getCode(),
                    PlatformErrorCode.BUILT_IN_PROTECTED.getMsg());
        }
    }
}
