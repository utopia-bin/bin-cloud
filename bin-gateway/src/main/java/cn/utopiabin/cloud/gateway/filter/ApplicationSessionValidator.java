package cn.utopiabin.cloud.gateway.filter;

import cn.utopiabin.cloud.platform.api.application.SsoApi;
import java.time.Duration;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** 网关应用会话校验器，通过平台契约校验服务端会话状态。 */
@Component
public class ApplicationSessionValidator {

  @DubboReference(check = false, timeout = 3000)
  private SsoApi ssoApi;

  public Mono<Boolean> valid(String token, String audience) {
    // Dubbo 同步调用必须离开 Netty 事件循环，避免认证请求阻塞网关工作线程。
    return Mono.fromCallable(() -> ssoApi.validateSession(token, audience))
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofSeconds(3))
        .onErrorReturn(false);
  }
}
