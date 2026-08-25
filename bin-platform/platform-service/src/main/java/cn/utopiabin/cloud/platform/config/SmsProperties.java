package cn.utopiabin.cloud.platform.config;

import cn.utopiabin.cloud.platform.model.enums.SmsScene;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "platform.sms")
public class SmsProperties {
    private String defaultProvider = "";
    private Duration codeTtl = Duration.ofMinutes(5);
    private Duration sendInterval = Duration.ofSeconds(60);
    private int maxVerifyAttempts = 5;
    private Map<SmsScene, String> templates = new EnumMap<>(SmsScene.class);
}
