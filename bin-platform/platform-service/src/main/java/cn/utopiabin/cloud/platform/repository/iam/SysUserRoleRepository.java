package cn.utopiabin.cloud.platform.repository.iam;

import cn.utopiabin.cloud.platform.entity.iam.SysUserRole;
import cn.utopiabin.cloud.platform.mapper.iam.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 用户角色关联 Repository
 *
 * @since 1.0
 */
@Repository
public class SysUserRoleRepository extends ServiceImpl<SysUserRoleMapper, SysUserRole> {

    /**
     * 为用户分配角色 (全量替换)
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long tenantId, Long userId, List<Long> roleIds) {
        removeByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        var list = roleIds.stream().map(roleId -> {
            var ur = new SysUserRole();
            ur.setTenantId(tenantId);
            ur.setTenantApplicationId(tenantId);
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        }).toList();
        saveBatch(list);
    }

    /**
     * 获取用户拥有的角色 ID 列表
     */
    public List<Long> getRoleIdsByUserId(Long userId) {
        var list = list(new LambdaQueryWrapper<SysUserRole>()
                .select(SysUserRole::getRoleId)
                .eq(SysUserRole::getUserId, userId));
        return list == null || list.isEmpty() ? Collections.emptyList()
                : list.stream().map(SysUserRole::getRoleId).toList();
    }

    /**
     * 删除用户的所有角色关联
     */
    public void removeByUserId(Long userId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    /**
     * 删除角色的所有用户关联
     */
    public void removeByRoleId(Long roleId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }
}
