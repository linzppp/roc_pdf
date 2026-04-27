package org.roc.practice.pdf.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学籍卡片 PDF 请求 DTO
 * 字段名与 PDF AcroForm 字段名保持一致，便于批量填写
 */
@Data
public class SCardPdfRequest {

    /** 业务唯一键，用于幂等去重（可选） */
    private String bizId;

    // ---------- 以下为 AcroForm 字段，字段名需与模板一致 ----------

    private String qu;
    private String schoolName;
    private String nationalId;
    private String name;
    private String eduId;
    private String levelName;
}
