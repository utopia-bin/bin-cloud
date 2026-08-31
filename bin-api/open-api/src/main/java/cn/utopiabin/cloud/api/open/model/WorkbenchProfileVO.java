package cn.utopiabin.cloud.api.open.model;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import cn.utopiabin.cloud.platform.model.vo.application.ApplicationProfileVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@Schema(description="学习工作台页面状态；不包含应用Access/Refresh Token")
public class WorkbenchProfileVO extends JsonSerializable {
    @Schema(description="服务端实时校验的应用身份、角色和菜单")
    private ApplicationProfileVO profile;
    @Schema(description="本次浏览器会话的CSRF令牌，仅保存在页面内存，写操作放X-CSRF-Token头")
    private String csrfToken;
}
