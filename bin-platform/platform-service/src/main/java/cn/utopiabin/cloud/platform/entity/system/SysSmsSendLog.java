package cn.utopiabin.cloud.platform.entity.system;

import cn.utopiabin.cloud.platform.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sms_send_log")
public class SysSmsSendLog extends BaseEntity {
    private String phone;
    private String scene;
    private String provider;
    private String templateCode;
    private Boolean success;
    private String requestId;
    private String errorCode;
    private String errorMessage;
    private Date sendTime;
}
