package org.roc.practice.pdf.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PDF 各步骤耗时统计记录器
 *
 * <p>每次记录写入 Redis Sorted Set：
 * <pre>
 *   Key:    pdf:timing:{templateName}:{step}
 *   Score:  耗时毫秒（double）
 *   Member: UUID（保证唯一）
 * </pre>
 *
 * <p>P95 分析示例（以 invoice 模板的 template_load 步骤为例）：
 * <pre>
 *   ZCARD pdf:timing:invoice:template_load          → 获取样本总数 N
 *   ZRANGE pdf:timing:invoice:template_load (floor(N*0.95)-1) (floor(N*0.95)-1) WITHSCORES
 *   ZRANGE pdf:timing:invoice:template_load -1 -1 WITHSCORES   → 最大值
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTimingRecorder {

    private static final String KEY_PREFIX = "pdf:timing:";

    private final StringRedisTemplate redis;

    @Value("${pdf.timing.enabled:true}")
    private boolean enabled;

    @Value("${pdf.timing.max-entries:10000}")
    private int maxEntries;

    @Value("${pdf.qps.ttl-seconds:600}")
    private int qpsTtlSeconds;

    /**
     * 记录一次步骤耗时。
     *
     * <p>通过 pipeline 一次网络往返执行：
     * <ol>
     *   <li>ZADD key millis uuid</li>
     *   <li>ZREMRANGEBYRANK key 0 -(maxEntries+2)  — 超限时从低分端裁剪</li>
     * </ol>
     *
     * @param templateName 模板名称，如 "invoice"
     * @param step         耗时步骤
     * @param millis       耗时毫秒
     */
    public void record(String templateName, PdfTimingStep step, long millis) {
        if (!enabled) {
            return;
        }
        String redisKey = KEY_PREFIX + templateName + ":" + step.key();
        String member = UUID.randomUUID().toString();
        try {
            redis.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public Object execute(RedisOperations operations) {
                    operations.opsForZSet().add(redisKey, member, (double) millis);
                    // 保留分数最高的 maxEntries 条；超出部分从低分端裁剪
                    operations.opsForZSet().removeRange(redisKey, 0, -(maxEntries + 2L));
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("[PDF-TIMING] Failed to record timing for {}/{}: {}", templateName, step.key(), e.getMessage());
        }
    }

    /**
     * 记录一次 PDF 生成完成，按秒计数。
     *
     * <p>Redis Key: {@code pdf:qps:{templateName}:{epoch_second}}，类型 String（计数器）。
     * TTL 到期后自动清理，无需手动关闭。
     *
     * <p>查询示例：
     * <pre>
     *   SCAN 0 MATCH pdf:qps:invoice:* COUNT 200
     *   GET  pdf:qps:invoice:1747123456
     * </pre>
     *
     * @param templateName 模板名称，如 "invoice"
     */
    public void recordQps(String templateName) {
        if (!enabled) {
            return;
        }
        String key = "pdf:qps:" + templateName + ":" + (System.currentTimeMillis() / 1000);
        try {
            redis.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public Object execute(RedisOperations operations) {
                    operations.opsForValue().increment(key);
                    operations.expire(key, qpsTtlSeconds, TimeUnit.SECONDS);
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("[PDF-QPS] Failed to record qps for {}: {}", templateName, e.getMessage());
        }
    }
}
