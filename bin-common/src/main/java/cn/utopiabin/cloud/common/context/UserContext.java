package cn.utopiabin.cloud.common.context;

import cn.utopiabin.cloud.common.utils.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 用户上下文数据模型 (不可变)
 * <p>
 * 携带由网关 JWT 鉴权后注入的用户身份信息, 通过请求头向下游服务传递:
 * <ul>
 *   <li>X-User-Id   → userId</li>
 *   <li>X-User-Name → username</li>
 *   <li>X-Tenant-Id → tenantId</li>
 *   <li>X-User-Roles → roles (逗号分隔)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户名 */
    private String username;

    /** 租户 ID (多租户数据隔离) */
    private String tenantId;

    /** 角色列表 */
    private List<String> roles;

    /**
     * 从请求头值快速构建
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param rolesStr 角色字符串 (逗号分隔, 可为空)
     * @return UserContext 实例
     */
    public static UserContext of(String userId, String username, String tenantId, String rolesStr) {
        return new UserContext(
                userId,
                username,
                tenantId,
                StrUtil.isBlank(rolesStr)
                        ? Collections.emptyList()
                        : List.of(rolesStr.split(","))
        );
    }

    /**
     * 判断当前上下文是否包含有效用户信息
     */
    public boolean isValid() {
        return StrUtil.isNotBlank(userId);
    }
}
