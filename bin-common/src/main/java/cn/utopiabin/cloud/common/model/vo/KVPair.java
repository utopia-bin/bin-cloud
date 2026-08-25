package cn.utopiabin.cloud.common.model.vo;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 通用键值对
 *
 * @author Bin
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通用键值对")
public class KVPair<K, V> extends JsonSerializable {

    @Schema(description = "键")
    private K key;
    @Schema(description = "与键关联的值")
    private V value;

    public static <K, V> KVPair<K, V> of(K key, V value) {
        return new KVPair<>(key, value);
    }

    public static <K, V> KVPair<K, V> entry(Map.Entry<K, V> entry) {
        return new KVPair<>(entry.getKey(), entry.getValue());
    }
}
