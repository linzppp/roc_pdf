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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

    @Autowired
    @Qualifier("pdfUploadExecutor")
    private Executor uploadExecutor;

    @Async("pdfTaskExecutor")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void execute(String type, T request, String taskId, String objectKey) {
        log.info("Starting PDF generation: type={}, taskId={}", type, taskId);
        final long startTime = System.currentTimeMillis();
        taskManager.updateStatus(taskId, TaskStatus.PROCESSING);
        try {
            PdfGenerator<T> generator = (PdfGenerator) registry.get(type);
            byte[] pdfBytes = generator.generate(request);

            // PDF 生成完成，在投递上传前打点，避免把 upload 排队等待时间算入生成耗时
            timingRecorder.record(type, PdfTimingStep.TOTAL, System.currentTimeMillis() - startTime);
            timingRecorder.recordQps(type);

            // pdf-gen 线程至此释放，upload 投递到独立线程池
            CompletableFuture.runAsync(() -> {
                try {
                    storageService.upload(objectKey, pdfBytes);
                    taskManager.updateDone(taskId);
                    log.info("Upload done: taskId={}, objectKey={}", taskId, objectKey);
                } catch (Exception e) {
                    log.error("Upload failed: taskId={}", taskId, e);
                    taskManager.updateFailed(taskId, e.getMessage());
                }
            }, uploadExecutor);

            log.info("PDF generation done, upload submitted: taskId={}", taskId);
        } catch (Exception e) {
            log.error("PDF generation failed: taskId={}", taskId, e);
            taskManager.updateFailed(taskId, e.getMessage());
        }
    }
}
