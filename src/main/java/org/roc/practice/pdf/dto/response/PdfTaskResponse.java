package org.roc.practice.pdf.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.roc.practice.pdf.common.enums.TaskStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfTaskResponse {

    /** 任务 ID，用于后续轮询 */
    private String taskId;

    /** 当前任务状态 */
    private TaskStatus status;

    /**
     * 文件最终存储的 URL（公开 bucket，URL 固定）
     * PENDING/PROCESSING 阶段 URL 已知但文件尚未生成，不可访问
     */
    private String url;

    /** 失败原因，status=FAILED 时有值 */
    private String error;
}
