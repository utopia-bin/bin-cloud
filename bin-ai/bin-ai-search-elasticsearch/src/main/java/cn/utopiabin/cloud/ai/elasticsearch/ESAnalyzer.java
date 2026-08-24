package cn.utopiabin.cloud.ai.elasticsearch;

/**
 * ES 分词器常量
 *
 * @since 1.0
 */
public final class ESAnalyzer {

    private ESAnalyzer() {
    }

    /**
     * IK 最大分词
     */
    public static final String IK_MAX_WORD = "ik_max_word";
    /**
     * IK 智能分词
     */
    public static final String IK_SMART = "ik_smart";
    /**
     * NGram 分词
     */
    public static final String NGRAM = "ngram";
    /**
     * 逗号分词
     */
    public static final String COMMA = "comma";
    /**
     * 分号分词
     */
    public static final String SEMICOLON = "semicolon";
}
