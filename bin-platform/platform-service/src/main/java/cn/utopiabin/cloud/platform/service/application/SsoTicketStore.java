package cn.utopiabin.cloud.platform.service.application;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SsoTicketStore {
    private final StringRedisTemplate redis;

    public void put(String code, String value) {
        redis.opsForValue()
                .set("platform:sso:code:" + SsoCrypto.hash(code), value, Duration.ofSeconds(60));
    }

    public String consume(String code) {
        return redis.opsForValue().getAndDelete("platform:sso:code:" + SsoCrypto.hash(code));
    }
}
