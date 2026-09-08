package cn.utopiabin.cloud.platform.entity.application;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 单点登录会话实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sso_session")
public class SysSsoSession extends BaseEntity {
  private String sessionId;
  private String parentSessionId;
  private Long applicationId;
  private Long tenantApplicationId;
  private Long userId;
  private Integer credentialVersion;
  private String status;
  private String refreshTokenHash;
  private LocalDateTime authTime;
  private LocalDateTime lastAccessAt;
  private LocalDateTime expireAt;
  private LocalDateTime revokedAt;
  private String revokeReason;
  private String clientIp;
  private String userAgent;
}
