package org.roc.practice.pdf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.roc.practice.pdf.common.enums.TaskStatus;
import org.roc.practice.pdf.generator.PdfGenerator;
import org.roc.practice.pdf.generator.PdfGeneratorRegistry;
import org.roc.practice.pdf.storage.StorageService;
import org.roc.practice.pdf.task.PdfTaskManager;
import org.roc.practice.pdf.template.PdfTimingRecorder;
import org.roc.practice.pdf.template.PdfTimingStep;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * PDF 异步执行器
 *
 * <p>独立 Bean 的原因：@Async 依赖 Spring AOP 代理，同类内调用无法生效。
 * 将异步方法抽到此类，由 PdfServiceImpl 注入调用，确保代理正确织入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfAsyncExecutor {

    private final PdfGeneratorRegistry registry;
    private final StorageService storageService;
    private final PdfTaskManager taskManager;
    private final PdfTimingRecorder timingRecorder;

    @Async("pdfTaskExecutor")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void execute(String type, T request, String taskId, String objectKey) {
        log.info("Starting PDF generation: type={}, taskId={}", type, taskId);
        taskManager.updateStatus(taskId, TaskStatus.PROCESSING);
        try {
            PdfGenerator<T> generator = (PdfGenerator) registry.get(type);
            byte[] pdfBytes = generator.generate(request);
            long t = System.currentTimeMillis();
            storageService.upload(objectKey, pdfBytes);
            timingRecorder.record(type, PdfTimingStep.MINIO_UPLOAD, System.currentTimeMillis() - t);
            timingRecorder.recordQps(type);
            taskManager.updateDone(taskId);
            log.info("PDF generation done: taskId={}, objectKey={}", taskId, objectKey);
        } catch (Exception e) {
            log.error("PDF generation failed: taskId={}", taskId, e);
            taskManager.updateFailed(taskId, e.getMessage());
        }
    }
}
