package cn.utopiabin.cloud.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI (Swagger 3) 配置
 *
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI platformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("平台基座服务 API")
                        .description("平台基座服务接口文档，包含租户管理、身份权限(IAM)、认证授权、系统字典与参数等模块")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bin")
                                .email("bin@utopiabin.cn")));
    }
}
