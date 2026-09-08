package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 全局权限资源目录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

  @Schema(description = "所属应用产品ID；平台IAM固定为平台壳应用")
  private Long applicationId;

  private String name;
  private String code;
  private String description;
  private Boolean available;
  private Integer sort;
}
