package cn.utopiabin.cloud.platform.entity.application;

import cn.utopiabin.cloud.platform.entity.base.LinkEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 单点登录审计事件实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sso_login_log")
public class SysSsoLoginLog extends LinkEntity {
  private Long applicationId;
  private Long userId;
  private String eventType;
  private Boolean success;
  private String failureCode;
  private String sessionId;
  private String traceId;
  private String clientIp;
  private String userAgent;
  private LocalDateTime eventTime;
}
