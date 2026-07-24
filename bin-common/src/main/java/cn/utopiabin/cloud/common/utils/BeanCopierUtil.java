package cn.utopiabin.cloud.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanCopier;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring CGLIB BeanCopier 工具封装
 *
 * @since 1.0.0
 */
@Slf4j
public final class BeanCopierUtil {

    private static final ConcurrentHashMap<String, BeanCopier> COPIER_CACHE = new ConcurrentHashMap<>();

    private BeanCopierUtil() {
    }

    /**
     * 拷贝对象属性 (浅拷贝,同名字段)
     *
     * @param source 源对象 (非 null)
     * @param target 目标对象 (非 null)
     */
    public static void copy(Object source, Object target) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        var sourceClass = source.getClass();
        var targetClass = target.getClass();
        var key = sourceClass.getName() + "->" + targetClass.getName();

        var copier = COPIER_CACHE.computeIfAbsent(key, k -> {
            try {
                return BeanCopier.create(sourceClass, targetClass, false);
            } catch (Exception e) {
                log.error("创建 BeanCopier 失败: {} -> {}", sourceClass.getSimpleName(), targetClass.getSimpleName(), e);
                return null;
            }
        });

        if (copier != null) {
            copier.copy(source, target, null);
        }
    }
}
