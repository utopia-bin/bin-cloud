package cn.utopiabin.cloud.common.json;


import cn.utopiabin.cloud.common.utils.BeanCopierUtil;
import cn.utopiabin.cloud.common.utils.JsonUtil;

import java.io.Serializable;

/**
 * 实体 JSON 序列化基类 —— 实体继承后自动获得:
 * <ul>
 *   <li>toString() → JSON 字符串</li>
 *   <li>toMap()  → 属性 Map</li>
 *   <li>copyTo()  → 属性拷贝到另一个实例</li>
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class JsonSerializable implements Serializable {

    /**
     * 输出 JSON 字符串
     */
    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }

    /**
     * 实体属性转 Map
     */
    public java.util.Map<String, Object> toMap() {
        return JsonUtil.toMap(this);
    }

    /**
     * 将当前实例属性拷贝到目标类型新实例 (浅拷贝,同名字段)
     *
     * @param targetClass 目标类型
     */
    public <T> T copyTo(Class<T> targetClass) {
        return JsonUtil.convert(this, targetClass);
    }

    /**
     * 将当前实例属性拷贝到已有实例 (CGLIB BeanCopier)
     *
     * @param target 目标实例
     */
    public void copyTo(Object target) {
        BeanCopierUtil.copy(this, target);
    }
}
