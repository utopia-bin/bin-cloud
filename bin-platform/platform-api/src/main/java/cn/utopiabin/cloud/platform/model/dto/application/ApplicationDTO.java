package cn.utopiabin.cloud.platform.model.dto.application;

import cn.utopiabin.cloud.common.json.JsonSerializable;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用产品保存参数，已发布编码与服务标识不可修改")
public class ApplicationDTO extends JsonSerializable {
    @Schema(description = "新增留空，编辑填写产品ID")
    private Long id;

    @Schema(description = "编辑时必填，防止并发覆盖")
    private Integer expectedVersion;

    @Schema(description = "稳定应用编码，同时作为Token受众")
    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9-]{1,63}")
    private String code;

    @Schema(description = "应用名称")
    @NotBlank
    @Size(max = 100)
    private String name;

    @Schema(description = "应用用途说明")
    @Size(max = 500)
    @NotNull
    private String description = "";

    @Schema(description = "可选HTTP图标URL或本站路径")
    @Size(max = 500)
    @NotNull
    private String iconUrl = "";

    @Schema(description = "应用后端发起SSO的入口，支持本站绝对路径")
    @NotBlank
    @Size(max = 500)
    private String entryUrl;

    @Schema(description = "唯一后端客户端标识，不以该字符串单独作为认证依据")
    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9-]{1,99}")
    private String serviceId;

    @Schema(description = "ENABLED启用、DISABLED停用、OFFLINE下架")
    @NotBlank
    @Pattern(regexp = "ENABLED|DISABLED|OFFLINE")
    @NotNull
    private String status = "ENABLED";

    @Schema(description = "是否允许授权码登录")
    private boolean ssoEnabled = true;

    @Schema(description = "排序值，越小越靠前")
    @Min(0)
    private int sort = 10;

    @Schema(description = "最多20条精确回调白名单")
    @Valid
    @NotNull
    @Size(max = 20)
    private List<@NotNull RedirectDTO> redirectUris = new ArrayList<>();
}
