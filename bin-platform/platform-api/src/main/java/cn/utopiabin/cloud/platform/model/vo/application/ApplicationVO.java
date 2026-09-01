package cn.utopiabin.cloud.platform.model.vo.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用产品详情，不返回客户端凭证或摘要")
public class ApplicationVO extends JsonSerializable {
    @Schema(description = "应用ID")
    private Long id;

    @Schema(description = "当前乐观锁版本")
    private Integer version;

    @Schema(description = "Token受众编码")
    private String code;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "应用简介")
    private String description;

    @Schema(description = "图标地址")
    private String iconUrl;

    @Schema(description = "应用登录发起入口")
    private String entryUrl;

    @Schema(description = "客户端标识")
    private String serviceId;

    @Schema(description = "ENABLED、DISABLED或OFFLINE")
    private String status;

    @Schema(description = "是否允许SSO")
    private boolean ssoEnabled;

    @Schema(description = "是否已配置客户端凭证")
    private boolean clientConfigured;

    @Schema(description = "排序值")
    private int sort;

    @Schema(description = "精确回调地址集合")
    private List<cn.utopiabin.cloud.platform.model.dto.application.RedirectDTO> redirectUris;
}
