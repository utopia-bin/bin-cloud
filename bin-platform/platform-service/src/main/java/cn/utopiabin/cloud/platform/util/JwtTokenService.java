package cn.utopiabin.cloud.platform.util;

import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.platform.config.JwtTokenProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT Token 服务 —— Spring Bean，替代静态 {@code JwtTokenUtil}
 * <p>
 * 与 gateway 的 {@code JwtUtil} 配对使用，双方共享同一密钥与 Payload 结构。
 * <p>
 * JWT Claims:
 * <ul>
 *   <li>userId   — 用户 ID</li>
 *   <li>username — 用户名</li>
 *   <li>tenantId — 租户 ID (多租户隔离)</li>
 *   <li>roles    — 角色编码列表</li>
 *   <li>iat / exp — 签发/过期时间</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtTokenProperties properties;

    /**
     * 构建 HMAC 签名密钥
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param roles    角色编码列表
     * @return JWT Token 字符串
     */
    public String generate(String userId, String username, String tenantId, List<String> roles) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiration = new Date(now + properties.getJwtExpiration() * 1000);

        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("tenantId", tenantId)
                .claim("roles", roles != null ? roles : List.of())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /**
     * 从 Token 获取剩余有效时间 (秒), 用于设置黑名单 TTL
     *
     * @param token JWT Token
     * @return 剩余秒数, 解析失败返回 0
     */
    public long getRemainingTtl(String token) {
        if (StrUtil.isBlank(token)) {
            return 0;
        }
        try {
            var payload = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date exp = payload.getExpiration();
            if (exp == null) {
                return 0;
            }
            long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            log.debug("解析 Token 过期时间失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 对 Token 取后 16 位作为黑名单索引 (与 gateway JwtAuthFilter 保持一致)
     */
    public String blacklistSuffix(String token) {
        return token.length() > 16 ? token.substring(token.length() - 16) : token;
    }
}
