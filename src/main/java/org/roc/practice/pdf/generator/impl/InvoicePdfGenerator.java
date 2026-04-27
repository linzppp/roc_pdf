package org.roc.practice.pdf.generator.impl;

import lombok.RequiredArgsConstructor;
import org.roc.practice.pdf.dto.request.InvoicePdfRequest;
import org.roc.practice.pdf.generator.PdfGenerator;
import org.roc.practice.pdf.template.PdfTemplateEngine;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 发票 PDF 生成器
 *
 * <p>模板文件：{pdf.template.path}/invoice.pdf
 * <p>新增生成器参照此类实现，无需改动其他代码
 */
@Component
@RequiredArgsConstructor
public class InvoicePdfGenerator implements PdfGenerator<InvoicePdfRequest> {

    private final PdfTemplateEngine engine;

    @Override
    public String type() {
        return "invoice";
    }

    @Override
    public Class<InvoicePdfRequest> requestType() {
        return InvoicePdfRequest.class;
    }

    @Override
    public byte[] generate(InvoicePdfRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("invoiceNo",    safeStr(req.getInvoiceNo()));
        fields.put("invoiceDate",  safeStr(req.getInvoiceDate()));
        fields.put("sellerName",   safeStr(req.getSellerName()));
        fields.put("sellerTaxNo",  safeStr(req.getSellerTaxNo()));
        fields.put("buyerName",    safeStr(req.getBuyerName()));
        fields.put("buyerTaxNo",   safeStr(req.getBuyerTaxNo()));
        fields.put("goodsName",    safeStr(req.getGoodsName()));
        fields.put("amount",       safeStr(req.getAmount()));
        fields.put("taxRate",      safeStr(req.getTaxRate()));
        fields.put("taxAmount",    safeStr(req.getTaxAmount()));
        fields.put("totalAmount",  safeStr(req.getTotalAmount()));
        fields.put("remark",       safeStr(req.getRemark()));

        return engine.fill("invoice", fields);
    }

    private String safeStr(Object value) {
        return Objects.toString(value, "");
    }
}
