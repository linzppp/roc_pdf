package org.roc.practice.pdf.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票 PDF 请求 DTO
 * 字段名与 PDF AcroForm 字段名保持一致，便于批量填写
 */
@Data
public class InvoicePdfRequest {

    /** 业务唯一键，用于幂等去重（可选） */
    private String bizId;

    // ---------- 以下为 AcroForm 字段，字段名需与模板一致 ----------

    private String invoiceNo;
    private String invoiceDate;
    private String sellerName;
    private String sellerTaxNo;
    private String buyerName;
    private String buyerTaxNo;
    private String goodsName;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remark;
}
