package org.roc.practice.pdf.service;

import org.roc.practice.pdf.dto.response.PdfTaskResponse;

/**
 * PDF 生成服务接口
 * Service 层与传输层（HTTP / RPC）解耦，可被 Controller 和未来 RPC Facade 共同调用
 */
public interface PdfService {

    /**
     * 提交 PDF 生成任务（异步）
     *
     * @param type        PDF 类型，对应 PdfGenerator.type()
     * @param requestBody 请求 JSON 字符串（由具体 Generator 自行反序列化）
     * @param bizId       业务唯一键，用于幂等去重（可为 null）
     * @return 任务信息，包含 taskId 和预计 URL
     */
    PdfTaskResponse submit(String type, String requestBody, String bizId);

    /**
     * 查询任务状态
     *
     * @param taskId 任务 ID
     * @return 任务信息，不存在时返回 null
     */
    PdfTaskResponse queryTask(String taskId);

    /**
     * 删除任务记录（不删除已生成的 MinIO 文件）
     *
     * @param taskId 任务 ID
     */
    void deleteTask(String taskId);
}
