package org.roc.practice.pdf.api.controller;

import lombok.RequiredArgsConstructor;
import org.roc.practice.pdf.api.facade.PdfFacade;
import org.roc.practice.pdf.common.result.R;
import org.roc.practice.pdf.dto.response.PdfTaskResponse;
import org.roc.practice.pdf.generator.PdfGeneratorRegistry;
import org.springframework.web.bind.annotation.*;

/**
 * PDF 生成 HTTP 接口
 *
 * <pre>
 * POST   /pdf/{type}           提交生成任务，立即返回 taskId + url
 * GET    /pdf/task/{taskId}    查询任务状态
 * DELETE /pdf/task/{taskId}    删除任务记录
 * </pre>
 */
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfFacade pdfFacade;
    private final PdfGeneratorRegistry registry;

    /**
     * 提交 PDF 生成任务
     *
     * @param type        URL 路径中的类型，如 invoice / contract
     * @param requestBody 请求体（JSON 字符串），由对应 Generator 反序列化
     * @param bizId       业务唯一键，用于幂等（可选 Header）
     */
    @PostMapping("/{type}")
    public R<PdfTaskResponse> submit(
            @PathVariable String type,
            @RequestBody String requestBody,
            @RequestHeader(value = "X-Biz-Id", required = false) String bizId) {
        if (!registry.contains(type)) {
            return R.notFound("Unsupported PDF type: " + type);
        }
        try {
            PdfTaskResponse resp = pdfFacade.submit(type, requestBody, bizId);
            return R.ok(resp);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询任务状态（调用方轮询此接口）
     */
    @GetMapping("/task/{taskId}")
    public R<PdfTaskResponse> queryTask(@PathVariable String taskId) {
        PdfTaskResponse task = pdfFacade.queryTask(taskId);
        if (task == null) {
            return R.notFound("Task not found: " + taskId);
        }
        return R.ok(task);
    }

    /**
     * 删除任务记录（不删除 MinIO 中已生成的文件）
     */
    @DeleteMapping("/task/{taskId}")
    public R<Void> deleteTask(@PathVariable String taskId) {
        pdfFacade.deleteTask(taskId);
        return R.ok(null);
    }
}
