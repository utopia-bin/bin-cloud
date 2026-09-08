package cn.utopiabin.cloud.platform.entity.application;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 应用实例成员授权实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_application")
public class SysUserApplication extends BaseEntity {
  private Long tenantApplicationId;
  private Long userId;
  private String status;
  private LocalDateTime effectiveAt;
  private LocalDateTime expireAt;
  private Long grantedBy;
  private LocalDateTime grantedAt;
  private String comment;
}
