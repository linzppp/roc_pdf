package org.roc.practice.pdf.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.roc.practice.pdf.common.enums.TaskStatus;
import org.roc.practice.pdf.dto.response.PdfTaskResponse;
import org.roc.practice.pdf.generator.PdfGenerator;
import org.roc.practice.pdf.generator.PdfGeneratorRegistry;
import org.roc.practice.pdf.service.PdfAsyncExecutor;
import org.roc.practice.pdf.service.PdfService;
import org.roc.practice.pdf.storage.StorageService;
import org.roc.practice.pdf.task.PdfTaskManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final PdfGeneratorRegistry registry;
    private final PdfTaskManager taskManager;
    private final StorageService storageService;
    private final PdfAsyncExecutor asyncExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public PdfTaskResponse submit(String type, String requestBody, String bizId) {
        // 幂等：相同 bizId 直接返回已有任务
        if (bizId != null && !bizId.isBlank()) {
            String existingTaskId = taskManager.findTaskIdByBizId(bizId);
            if (existingTaskId != null) {
                log.info("Idempotent hit: bizId={} -> taskId={}", bizId, existingTaskId);
                PdfTaskResponse existing = taskManager.getTask(existingTaskId);
                if (existing != null) {
                    return existing;
                }
            }
        }

        PdfGenerator<?> generator = registry.get(type);
        if (generator == null) {
            throw new IllegalArgumentException("Unknown PDF type: " + type);
        }

        String taskId    = UUID.randomUUID().toString();
        String objectKey = buildObjectKey(taskId);
        String url       = storageService.buildUrl(objectKey);

        taskManager.createTask(taskId, url, bizId);

        // 反序列化请求体
        Object request = deserialize(requestBody, generator.requestType());

        // 异步执行（调用独立 Bean，确保 @Async AOP 代理生效）
        asyncExecutor.execute(type, request, taskId, objectKey);

        return new PdfTaskResponse(taskId, TaskStatus.PENDING, url, null);
    }

    @Override
    public PdfTaskResponse queryTask(String taskId) {
        return taskManager.getTask(taskId);
    }

    @Override
    public void deleteTask(String taskId) {
        taskManager.deleteTask(taskId);
    }

    // ------------------------------------------------------------------ helper

    private String buildObjectKey(String taskId) {
        return DATE_PATH.format(LocalDate.now()) + "/" + taskId + ".pdf";
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid request body: " + e.getMessage(), e);
        }
    }
}
