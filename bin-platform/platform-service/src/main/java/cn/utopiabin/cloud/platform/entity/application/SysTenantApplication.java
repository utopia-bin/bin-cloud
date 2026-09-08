package cn.utopiabin.cloud.platform.entity.application;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户应用开通实例实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_application")
public class SysTenantApplication extends BaseEntity {
  private Long applicationId;
  private String status;
  private String accessPolicy;
  private String entryUrlOverride;
  private LocalDateTime openedAt;
  private LocalDateTime effectiveAt;
  private LocalDateTime expireAt;
  private LocalDateTime suspendedAt;
  private LocalDateTime closedAt;
  private String configJson;
  private String comment;
}
