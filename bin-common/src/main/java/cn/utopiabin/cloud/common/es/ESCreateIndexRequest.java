package cn.utopiabin.cloud.common.es;

import cn.utopiabin.cloud.common.json.JsonSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;

/**
 * ES 创建索引参数
 *
 * @since 1.0
 */
@Getter
@Setter
@Accessors(chain = true)
public class ESCreateIndexRequest extends JsonSerializable {
    /**
     * 索引名称
     */
    private String name;

    /**
     * 索引版本
     */
    private String version = "";

    /**
     * 分片数量
     */
    private Integer shards = 1;

    /**
     * 副本数量
     */
    private Integer replicas = 1;

    /**
     * 刷新间隔
     */
    private String refreshInterval = "1s";

    /**
     * 索引映射
     */
    private TypeMapping mapping;
}
