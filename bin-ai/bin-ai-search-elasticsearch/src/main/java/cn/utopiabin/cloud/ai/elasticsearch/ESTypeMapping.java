package cn.utopiabin.cloud.ai.elasticsearch;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.util.ObjectBuilder;

import java.util.function.Function;

/**
 * ES 字段类型映射 —— 用于构建索引 Mappings
 *
 * @since 1.0
 */
public final class ESTypeMapping {

    private ESTypeMapping() {
    }

    /**
     * 精准匹配 (keyword, 超64不索引)
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> ID =
            p -> p.keyword(k -> k.docValues(true).ignoreAbove(64));

    /**
     * 全文检索 (ik_max_word 索引 + ik_smart 搜索)
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> TEXT =
            p -> p.text(t -> t.analyzer(ESAnalyzer.IK_MAX_WORD).searchAnalyzer(ESAnalyzer.IK_SMART));

    /**
     * 精准匹配 (keyword)
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> KEYWORD =
            p -> p.keyword(k -> k);

    /**
     * 日期类型
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> DATE =
            p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||strict_date_optional_time||epoch_millis"));

    /**
     * 不索引文本
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> TEXT_NO_INDEX =
            p -> p.text(t -> t.index(false));

    /**
     * 排序整数
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> SORT =
            p -> p.integer(i -> i.docValues(true).index(true).nullValue(0));

    /**
     * 逗号分隔字段
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> COMMA =
            p -> p.text(t -> t.analyzer(ESAnalyzer.COMMA).searchAnalyzer(ESAnalyzer.COMMA));

    /**
     * 分号分隔字段
     */
    public static final Function<Property.Builder, ObjectBuilder<Property>> SEMICOLON =
            p -> p.text(t -> t.analyzer(ESAnalyzer.SEMICOLON).searchAnalyzer(ESAnalyzer.SEMICOLON));
}
