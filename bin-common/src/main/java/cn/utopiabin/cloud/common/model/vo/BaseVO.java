package cn.utopiabin.cloud.common.model.vo;

import cn.utopiabin.cloud.common.json.JsonSerializable;
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
public abstract class BaseVO extends JsonSerializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /** 乐观锁版本号 */
    private Integer version;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModify;

    /**
     * 创建人
     */
    private String createUser;

    /**
     * 修改人
     */
    private String modifyUser;
}
