package cn.utopiabin.cloud.platform.entity.application;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 应用精确回调地址实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_application_redirect_uri", excludeProperty = "tenantId")
public class SysApplicationRedirect extends BaseEntity {
  private Long applicationId;
  private String environment;
  private String redirectUri;
  private String logoutUri;
  private Boolean available;
}
