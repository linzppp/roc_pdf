package org.roc.practice.pdf.template;

/**
 * PDF 生成各阶段耗时统计步骤枚举
 *
 * <p>使用 {@link #key()} 而非 {@link #name()} 作为 Redis key 的一部分，
 * 避免枚举重命名导致历史数据 key 不一致。
 */
public enum PdfTimingStep {

    TOTAL("total");

    private final String key;

    PdfTimingStep(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
