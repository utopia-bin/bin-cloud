package cn.utopiabin.cloud.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录安全配置
 * <p>
 * 对应配置:
 * <pre>
 * platform:
 *   login-security:
 *     max-fail-count: 5            # 最大失败次数
 *     lock-duration-seconds: 900   # 锁定时长 (秒)
 *     fail-count-window-seconds: 1800  # 失败计数窗口 (秒)
 *     delay-base-ms: 200           # 失败延迟基数 (毫秒)
 *     max-delay-ms: 3000           # 失败延迟上限 (毫秒)
 *     password-min-length: 8       # 密码最小长度
 *     password-require-special: false  # 是否要求特殊字符
 * </pre>
 *
 * @since 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform.login-security")
public class LoginSecurityProperties {

    /**
     * 窗口期内允许的最大密码失败次数，达到后锁定账号
     */
    private int maxFailCount = 5;

    /**
     * 账号锁定时长 (秒)
     */
    private long lockDurationSeconds = 900;

    /**
     * 失败计数统计窗口 (秒)
     */
    private long failCountWindowSeconds = 1800;

    /**
     * 每次登录失败后的基础延迟 (毫秒)，按失败次数线性递增
     */
    private long delayBaseMs = 200;

    /**
     * 登录失败延迟上限 (毫秒)
     */
    private long maxDelayMs = 3000;

    /**
     * 密码最小长度
     */
    private int passwordMinLength = 8;

    /**
     * 密码是否必须包含特殊字符
     */
    private boolean passwordRequireSpecial = false;
}
