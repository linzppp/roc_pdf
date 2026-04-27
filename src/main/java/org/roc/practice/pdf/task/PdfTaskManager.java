package org.roc.practice.pdf.task;

import lombok.RequiredArgsConstructor;
import org.roc.practice.pdf.common.enums.TaskStatus;
import org.roc.practice.pdf.dto.response.PdfTaskResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 任务状态管理：Redis Hash
 * Key:   pdf:task:{taskId}
 * Field: status / url / bizId / createdAt / error
 */
@Component
@RequiredArgsConstructor
public class PdfTaskManager {

    private static final String TASK_KEY_PREFIX  = "pdf:task:";
    private static final String BIZID_KEY_PREFIX = "pdf:bizid:";

    private final StringRedisTemplate redis;

    @Value("${pdf.task.ttl-days:7}")
    private long ttlDays;

    // ------------------------------------------------------------------ create

    public void createTask(String taskId, String url, String bizId) {
        String taskKey = taskKey(taskId);
        redis.opsForHash().putAll(taskKey, Map.of(
                "status", TaskStatus.PENDING.name(),
                "url", url,
                "bizId", bizId != null ? bizId : "",
                "createdAt", Instant.now().toString()
        ));
        redis.expire(taskKey, ttlDays, TimeUnit.DAYS);

        if (bizId != null && !bizId.isBlank()) {
            String bizKey = BIZID_KEY_PREFIX + bizId;
            redis.opsForValue().set(bizKey, taskId, ttlDays, TimeUnit.DAYS);
        }
    }

    // ------------------------------------------------------------------ update

    public void updateStatus(String taskId, TaskStatus status) {
        redis.opsForHash().put(taskKey(taskId), "status", status.name());
    }

    public void updateDone(String taskId) {
        redis.opsForHash().put(taskKey(taskId), "status", TaskStatus.DONE.name());
    }

    public void updateFailed(String taskId, String error) {
        redis.opsForHash().putAll(taskKey(taskId), Map.of(
                "status", TaskStatus.FAILED.name(),
                "error", error != null ? error : ""
        ));
    }

    // ------------------------------------------------------------------ query

    public PdfTaskResponse getTask(String taskId) {
        Map<Object, Object> entries = redis.opsForHash().entries(taskKey(taskId));
        if (entries.isEmpty()) {
            return null;
        }
        PdfTaskResponse resp = new PdfTaskResponse();
        resp.setTaskId(taskId);
        resp.setStatus(TaskStatus.valueOf((String) entries.get("status")));
        resp.setUrl((String) entries.get("url"));
        resp.setError((String) entries.get("error"));
        return resp;
    }

    /** 幂等查重：bizId 是否已有对应 taskId */
    public String findTaskIdByBizId(String bizId) {
        if (bizId == null || bizId.isBlank()) {
            return null;
        }
        return redis.opsForValue().get(BIZID_KEY_PREFIX + bizId);
    }

    // ------------------------------------------------------------------ delete

    public void deleteTask(String taskId) {
        redis.delete(taskKey(taskId));
    }

    // ------------------------------------------------------------------ helper

    private String taskKey(String taskId) {
        return TASK_KEY_PREFIX + taskId;
    }
}
