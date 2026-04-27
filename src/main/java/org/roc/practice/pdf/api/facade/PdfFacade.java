package org.roc.practice.pdf.api.facade;

import org.roc.practice.pdf.dto.response.PdfTaskResponse;

/**
 * PDF 服务 Facade 接口
 *
 * <p>当前由 {@link org.roc.practice.pdf.api.controller.PdfController} 通过 HTTP 暴露。
 * 未来扩展 RPC（Dubbo / gRPC 等）时，只需新增一个实现类，Service 层代码无需修改。
 *
 * <pre>
 * 当前：PdfController → PdfService
 * 未来：DubboProvider → PdfService  （复用相同 Service 实现）
 * </pre>
 */
public interface PdfFacade {

    /**
     * 提交 PDF 生成任务
     *
     * @param type        PDF 类型（如 invoice / contract）
     * @param requestBody 请求 JSON
     * @param bizId       业务唯一键，用于幂等（可为 null）
     */
    PdfTaskResponse submit(String type, String requestBody, String bizId);

    /** 查询任务状态 */
    PdfTaskResponse queryTask(String taskId);

    /** 删除任务记录 */
    void deleteTask(String taskId);
}
