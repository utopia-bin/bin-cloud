package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.platform.constant.CacheConstants;
import cn.utopiabin.cloud.platform.model.vo.iam.UserPermissionVO;
import cn.utopiabin.cloud.platform.repository.iam.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 权限聚合服务
 *
 * <p>统一管理用户权限数据的查询与缓存。通过 JOIN 查询优化 N+1 问题， 通过 Spring Cache 缓存用户权限聚合结果，减少登录/currentUser 的 DB 访问。
 *
 * <p>缓存策略:
 *
 * <ul>
 *   <li>缓存名: {@link CacheConstants#USER_PERM}, Key: userId, TTL: 30 分钟
 *   <li>角色/菜单变更时调用 {@link #evictAllUserPermissions()} 全量失效
 *   <li>用户角色变更时调用 {@link #evictUserPermission(Long)} 精准失效
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

  private final UserPermissionRepository repository;

  /**
   * 获取用户权限聚合 (含角色列表、菜单列表、菜单树)
   *
   * <p>缓存命中时不访问数据库；未命中时分别查询角色、权限和菜单投影。
   *
   * @param userId 用户 ID
   * @return 用户权限聚合
   */
  @Cacheable(value = CacheConstants.USER_PERM, key = "#userId")
  public UserPermissionVO getUserPermissions(Long userId) {
    return repository.getUserPermissions(userId);
  }

  /** 服务端权限判定，禁止以菜单是否可见代替授权。 */
  public boolean hasPermission(Long userId, String permissionCode) {
    if (permissionCode == null || permissionCode.isBlank()) {
      return false;
    }
    var codes = getUserPermissions(userId).getPermissionCodes();
    return codes.contains("*") || codes.contains(permissionCode);
  }

  /**
   * 失效指定用户的权限缓存
   *
   * @param userId 用户 ID
   */
  @CacheEvict(value = CacheConstants.USER_PERM, key = "#userId")
  public void evictUserPermission(Long userId) {
    log.debug("失效用户权限缓存: userId={}", userId);
  }

  /**
   * 失效所有用户的权限缓存
   *
   * <p>在角色/菜单变更时调用，因为可能影响多个用户。
   */
  @CacheEvict(value = CacheConstants.USER_PERM, allEntries = true)
  public void evictAllUserPermissions() {
    log.debug("失效所有用户权限缓存");
  }
}
