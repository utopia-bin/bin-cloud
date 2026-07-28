package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.model.vo.PageResult;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.constant.PlatformErrorCode;
import cn.utopiabin.cloud.platform.entity.iam.SysRole;
import cn.utopiabin.cloud.platform.entity.iam.SysUser;
import cn.utopiabin.cloud.platform.mapper.iam.SysUserMapper;
import cn.utopiabin.cloud.platform.model.dto.common.BatchDeleteDTO;
import cn.utopiabin.cloud.platform.model.dto.iam.*;
import cn.utopiabin.cloud.platform.model.vo.iam.SysRoleVO;
import cn.utopiabin.cloud.platform.model.vo.iam.SysUserVO;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRepository;
import cn.utopiabin.cloud.platform.repository.iam.SysUserRoleRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 系统用户服务
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    public void create(SysUserCreateDTO dto) {
        var username = dto.getUsername().trim();
        if (userRepository.countByField(SysUser::getUsername, username, null) > 0) {
            throw new BizException(PlatformErrorCode.USER_DUPLICATE.getCode(),
                    PlatformErrorCode.USER_DUPLICATE.getMsg());
        }

        var user = dto.copyTo(SysUser.class);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setSort(Optional.ofNullable(dto.getSort()).orElse(10));
        user.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(true));
        user.setGender(Optional.ofNullable(dto.getGender()).orElse(0));
        user.setRealName(StrUtil.defaultIfBlank(dto.getRealName(), ""));
        user.setPhone(StrUtil.defaultIfBlank(dto.getPhone(), ""));
        user.setEmail(StrUtil.defaultIfBlank(dto.getEmail(), ""));
        user.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserUpdateDTO dto) {
        var user = userRepository.getOrThrow(dto.getId());
        var username = dto.getUsername().trim();
        if (userRepository.countByField(SysUser::getUsername, username, dto.getId()) > 0) {
            throw new BizException(PlatformErrorCode.USER_DUPLICATE.getCode(),
                    PlatformErrorCode.USER_DUPLICATE.getMsg());
        }

        user.setUsername(username);
        user.setRealName(StrUtil.defaultIfBlank(dto.getRealName(), ""));
        user.setPhone(StrUtil.defaultIfBlank(dto.getPhone(), ""));
        user.setEmail(StrUtil.defaultIfBlank(dto.getEmail(), ""));
        user.setGender(Optional.ofNullable(dto.getGender()).orElse(user.getGender()));
        user.setSort(Optional.ofNullable(dto.getSort()).orElse(user.getSort()));
        user.setAvailable(Optional.ofNullable(dto.getAvailable()).orElse(user.getAvailable()));
        user.setComment(StrUtil.defaultIfBlank(dto.getComment(), ""));
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.updateById(user);

        permissionService.evictUserPermission(dto.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        userRepository.getOrThrow(id);
        userRoleRepository.removeByUserId(id);
        userRepository.removeById(id);
        permissionService.evictUserPermission(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            userRoleRepository.removeByUserId(id);
            permissionService.evictUserPermission(id);
        }
        userRepository.removeByIds(dto.getIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id, Boolean available) {
        var user = userRepository.getOrThrow(id);
        user.setAvailable(available);
        userRepository.updateById(user);
        permissionService.evictUserPermission(id);
    }

    public SysUserVO get(Long id) {
        return userRepository.getOrThrow(id).copyTo(SysUserVO.class);
    }

    public PageResult<SysUserVO> page(SysUserPageQuery query) {
        Page<SysUser> page = userRepository.page(query);
        var records = page.getRecords().stream()
                .map(u -> u.copyTo(SysUserVO.class))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public List<SysUserVO> list(SysUserListQuery query) {
        return userRepository.list(query).stream()
                .map(u -> u.copyTo(SysUserVO.class))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(SysUserAssignRolesDTO dto) {
        userRepository.getOrThrow(dto.getUserId());
        userRoleRepository.assignRoles(dto.getUserId(), dto.getRoleIds());
        permissionService.evictUserPermission(dto.getUserId());
    }

    public List<SysRoleVO> getRoles(Long userId) {
        List<SysRole> roles = userMapper.selectRolesByUserId(userId);
        return roles.stream()
                .map(r -> r.copyTo(SysRoleVO.class))
                .toList();
    }

    public boolean existsByUsername(String username) {
        return userRepository.exists(SysUser::getUsername, username);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        var user = userRepository.getOrThrow(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(user);
    }
}
