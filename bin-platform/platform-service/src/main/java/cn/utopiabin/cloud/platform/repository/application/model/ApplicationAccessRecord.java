package cn.utopiabin.cloud.platform.repository.application.model;

import java.time.LocalDateTime;
import lombok.Data;

/** 用户访问租户应用时所需的完整持久化边界。 */
@Data
public class ApplicationAccessRecord {
  private Long id;
  private Integer version;
  private Long tenantId;
  private Long applicationId;
  private Long userId;
  private String applicationCode;
  private String applicationName;
  private String productStatus;
  private String status;
  private String accessPolicy;
  private Boolean tenantAvailable;
  private Integer tenantDeleted;
  private LocalDateTime tenantExpire;
  private Boolean userAvailable;
  private Integer userDeleted;
  private String username;
  private Integer credentialVersion;
  private LocalDateTime effectiveAt;
  private LocalDateTime expireAt;
  private LocalDateTime grantExpire;
  private Boolean ssoEnabled;
  private String serviceId;
  private String entryUrl;
  private String iconUrl;
}
