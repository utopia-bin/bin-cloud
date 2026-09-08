package cn.utopiabin.cloud.platform.entity.iam;

import cn.utopiabin.cloud.platform.entity.base.LinkEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户角色与全局权限资源的关联。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class SysRolePermission extends LinkEntity {

  @Schema(description = "所属应用产品ID；平台IAM固定为平台壳应用")
  private Long applicationId;

  private Long roleId;
  private Long permissionId;
}
