package cn.utopiabin.cloud.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.TimeZone;

/** HTTP JSON preserves integers outside JavaScript's exact integer range. */
@AutoConfiguration
@ConditionalOnClass(Jackson2ObjectMapperBuilderCustomizer.class)
public class WebJsonAutoConfiguration {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer webJsonCustomizer() {
        return builder -> {
            var serializer = new JsonSerializer<Long>() {
                @Override
                public void serialize(Long value, JsonGenerator generator, SerializerProvider provider)
                        throws IOException {
                    if (value > 9_007_199_254_740_991L || value < -9_007_199_254_740_991L) {
                        generator.writeString(value.toString());
                    } else {
                        generator.writeNumber(value);
                    }
                }
            };
            builder.serializerByType(Long.class, serializer);
            builder.serializerByType(Long.TYPE, serializer);
            builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        };
    }
}
