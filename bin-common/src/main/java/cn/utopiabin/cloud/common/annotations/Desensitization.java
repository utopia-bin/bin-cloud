package cn.utopiabin.cloud.common.annotations;

import cn.utopiabin.cloud.common.json.DesensitizedSerialize;
import cn.utopiabin.cloud.common.model.enums.DesensitizeType;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段脱敏注解 —— 加在需要脱敏的 String 字段上, Jackson 序列化时自动脱敏
 *
 * <pre>{@code
 *   @Desensitization(type = DesensitizeType.MOBILE_PHONE)
 *   private String phone;  // 13812341234 → 138****1234
 * }</pre>
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizedSerialize.class)
public @interface Desensitization {
    /**
     * 脱敏类型
     */
    DesensitizeType type() default DesensitizeType.CUSTOM;

    /**
     * 脱敏起始位置 (包含)
     */
    int startInclude() default 0;

    /**
     * 脱敏结束位置 (不包含)
     */
    int endExclude() default 0;
}
