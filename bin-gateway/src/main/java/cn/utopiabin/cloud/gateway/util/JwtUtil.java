package cn.utopiabin.cloud.gateway.util;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import cn.utopiabin.cloud.common.utils.StrUtil;
import cn.utopiabin.cloud.gateway.model.JwtPayload;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 工具类 —— 基于 JJWT 实现 Token 解析、验签、过期判断
 *
 * @since 1.0.0
 */
@Slf4j
public final class JwtUtil {

    private JwtUtil() {
    }

    /**
     * 构建 HMAC 签名密钥
     *
     * @param secret 密钥字符串 (至少 256 bit)
     */
    public static SecretKey getKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析 Token 并校验签名、过期时间、租户身份
     *
     * @param token  JWT Token
     * @param secret 签名密钥
     * @return JWT 载荷, 解析失败返回 null
     */
    public static JwtPayload parse(String token, String secret) {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey(secret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            JwtPayload payload = new JwtPayload();
            payload.setUserId(claims.get("userId", String.class));
            payload.setUsername(claims.get("username", String.class));
            payload.setTenantId(claims.get("tenantId", String.class));
            payload.setIat(claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() / 1000 : 0);
            payload.setExp(claims.getExpiration() != null ? claims.getExpiration().getTime() / 1000 : 0);

            // roles 可能是 JSON 数组, 转成 List
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof java.util.List<?> list) {
                payload.setRoles(list.stream().map(Object::toString).toList());
            }

            // 租户 ID 校验: 多租户平台 Token 必须包含租户身份
            if (StrUtil.isBlank(payload.getUserId()) || StrUtil.isBlank(payload.getUsername())
                    || StrUtil.isBlank(payload.getTenantId())) {
                log.warn("JWT Token 缺少必要身份信息: userId={}, tenantId={}",
                        payload.getUserId(), payload.getTenantId());
                return null;
            }

            // 检查是否过期
            if (payload.isExpired()) {
                log.debug("JWT Token 已过期: userId={}, tenantId={}", payload.getUserId(), payload.getTenantId());
                return null;
            }

            return payload;
        } catch (ExpiredJwtException e) {
            log.debug("JWT Token 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的 JWT 格式: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("JWT 格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            log.debug("JWT 签名校验失败: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("JWT 解析异常: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT 解析未知异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 Authorization 头提取 Token (去除 Bearer 前缀)
     *
     * @param authorization Authorization 头值
     * @return 纯 Token, 无效时返回 null
     */
    public static String extractToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return null;
        }
        if (authorization.length() < CommonConstants.BEARER_PREFIX_LENGTH
                || !authorization.regionMatches(true, 0, CommonConstants.BEARER_PREFIX,
                0, CommonConstants.BEARER_PREFIX_LENGTH)) {
            return null;
        }
        String token = authorization.substring(CommonConstants.BEARER_PREFIX_LENGTH).trim();
        return token.isEmpty() ? null : token;
    }
}
