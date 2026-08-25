package cn.utopiabin.cloud.common.model.vo;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 展示公共基类
 *
 * @author Bin
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "接口响应对象的通用审计信息")
public abstract class BaseVO extends JsonSerializable {
    /**
     * 主键
     */
    @Schema(description = "资源主键 ID", example = "1")
    private Long id;

    /**
     * 租户 ID
     */
    @Schema(description = "资源所属租户 ID", example = "1")
    private Long tenantId;

    /** 乐观锁版本号 */
    @Schema(description = "乐观锁版本号", example = "1")
    private Integer version;

    /**
     * 创建时间
     */
    @Schema(description = "资源创建时间", example = "2026-08-25T09:30:00+08:00")
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @Schema(description = "资源最后更新时间", example = "2026-08-25T10:00:00+08:00")
    private Date gmtModify;

    /**
     * 创建人
     */
    @Schema(description = "创建人标识", example = "admin")
    private String createUser;

    /**
     * 修改人
     */
    @Schema(description = "最后修改人标识", example = "admin")
    private String modifyUser;
}
