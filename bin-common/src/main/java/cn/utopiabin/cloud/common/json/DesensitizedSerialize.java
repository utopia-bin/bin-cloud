package cn.utopiabin.cloud.common.json;

import cn.utopiabin.cloud.common.annotations.Desensitization;
import cn.utopiabin.cloud.common.model.enums.DesensitizeType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * Jackson 脱敏序列化器
 *
 * @since 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
public class DesensitizedSerialize extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;
    private int startInclude;
    private int endExclude;

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }
        if (!String.class.equals(property.getType().getRawClass())) {
            return prov.findValueSerializer(property.getType(), property);
        }
        Desensitization anno = property.getAnnotation(Desensitization.class);
        if (anno == null) {
            anno = property.getContextAnnotation(Desensitization.class);
        }
        if (anno == null) {
            return prov.findValueSerializer(property.getType(), property);
        }
        return new DesensitizedSerialize(anno.type(), anno.startInclude(), anno.endExclude());
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider prov) throws IOException {
        gen.writeString(type.mask(value, startInclude, endExclude));
    }
}
