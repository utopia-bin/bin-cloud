package cn.utopiabin.cloud.gateway.model;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * JWT Token 载荷模型
 *
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JwtPayload extends JsonSerializable {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 租户 ID (多租户隔离)
     */
    private String tenantId;

    /**
     * 用户角色列表
     */
    private List<String> roles;

    /**
     * Token 签发时间 (秒级时间戳)
     */
    private long iat;

    /**
     * Token 过期时间 (秒级时间戳)
     */
    private long exp;

    /**
     * Token 是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 > exp;
    }
}
