package cn.utopiabin.cloud.ai.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * ES 查询构建工具
 *
 * @since 1.0
 */
public final class ESQuery {

    private ESQuery() {
    }

    /**
     * 多字段模糊匹配
     */
    public static Query multiMatch(List<String> fields, String value) {
        return Query.of(q -> q.multiMatch(mm -> mm
                .query(value).fields(fields).type(TextQueryType.BestFields)
                .fuzziness("AUTO").prefixLength(1).maxExpansions(50).fuzzyTranspositions(true)));
    }

    /**
     * 前缀匹配
     */
    public static Query prefix(String field, String value) {
        return Query.of(q -> q.prefix(p -> p.field(field).value(value)));
    }

    /**
     * 通配符匹配
     */
    public static Query wildcard(String field, String value) {
        return Query.of(q -> q.wildcard(w -> w.field(field).value("*" + value + "*")));
    }

    /**
     * 多值精准匹配
     */
    public static Query terms(String field, List<?> values) {
        return Query.of(q -> q.terms(t -> t.field(field)
                .terms(ts -> ts.value(values.stream().map(FieldValue::of).toList()))));
    }

    /**
     * 不等于
     */
    public static Query notEquals(String field, Object value) {
        return Query.of(q -> q.bool(b -> b.mustNot(m -> m.term(t -> t.field(field).value(FieldValue.of(value))))));
    }

    /**
     * 字段为 null
     */
    public static Query isNull(String field) {
        return Query.of(q -> q.bool(b -> b.mustNot(m -> m.exists(e -> e.field(field)))));
    }

    /**
     * 字段不为 null
     */
    public static Query isNotNull(String field) {
        return Query.of(q -> q.exists(e -> e.field(field)));
    }

    /**
     * 字段不为 null 且不等于某值
     */
    public static Query isNotNullAndValue(String field, Object value) {
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.exists(e -> e.field(field.replace(".keyword", ""))))
                .mustNot(m -> m.term(t -> t.field(field).value(FieldValue.of(value))))));
    }

    /**
     * 字段为 null 或等于某值
     */
    public static Query isNullOrValue(String field, Object value) {
        return Query.of(q -> q.bool(b -> b
                .should(
                        BoolQuery.of(or -> or.mustNot(mn -> mn.exists(e -> e.field(field.replace(".keyword", "")))))._toQuery(),
                        Query.of(t -> t.term(TermQuery.of(term -> term.field(field).value(FieldValue.of(value)))))
                ).minimumShouldMatch("1")));
    }

    /**
     * 时间范围查询
     */
    public static Query dateRange(String field, LocalDateTime start, LocalDateTime end) {
        return Query.of(q -> q.range(r -> r.date(d -> d.field(field)
                .gte(start != null ? String.valueOf(toEpochMilli(start)) : null)
                .lte(end != null ? String.valueOf(toEpochMilli(end)) : null))));
    }

    private static long toEpochMilli(LocalDateTime dt) {
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
