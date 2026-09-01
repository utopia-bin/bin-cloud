package cn.utopiabin.cloud.platform.entity.application;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 应用产品目录实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_application", excludeProperty = "tenantId")
public class SysApplication extends BaseEntity {

    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private String entryUrl;
    private String serviceId;
    private String clientSecretHash;
    private String status;
    private Boolean ssoEnabled;
    private Integer sort;
}
