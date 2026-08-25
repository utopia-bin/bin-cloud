package cn.utopiabin.cloud.gateway.model;

import java.util.List;
import java.util.Set;

/**
 * 网关专属常量
 * <p>
 * 通用常量 (请求头 Key、Token 参数等) 已迁移至 {@link cn.utopiabin.cloud.common.constant.CommonConstants},
 * 本类仅保留网关模块特有的配置。
 *
 * @since 1.0.0
 */
public final class GatewayConstants {

    private GatewayConstants() {
    }

    // ==================== 鉴权白名单路径 (Ant 风格) ====================

    /** 无需鉴权的路径集合 */
    public static final Set<String> WHITE_PATHS = Set.of(
            "/auth/login",
            "/auth/phone/register",
            "/auth/phone/login",
            "/auth/phone/password",
            "/sms/code",
            "/public/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            "/actuator/**"
    );

    /** 不记录详细日志的健康检查路径 */
    public static final List<String> SKIP_LOG_PATHS = List.of(
            "/actuator/**",
            "/favicon.ico"
    );

    // ==================== Redis Key 前缀 ====================

    /** Token 黑名单 Redis Key 前缀 (主动注销/踢人下线) */
    public static final String TOKEN_BLACKLIST_PREFIX = "gateway:token:blacklist:";

    /** 限流 Redis Key 前缀 */
    public static final String RATE_LIMIT_PREFIX = "gateway:rate-limit:";
}
