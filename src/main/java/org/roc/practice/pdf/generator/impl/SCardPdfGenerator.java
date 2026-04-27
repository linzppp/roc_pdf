package org.roc.practice.pdf.generator.impl;

import lombok.RequiredArgsConstructor;
import org.roc.practice.pdf.dto.request.SCardPdfRequest;
import org.roc.practice.pdf.generator.PdfGenerator;
import org.roc.practice.pdf.template.PdfTemplateEngine;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 学籍卡片 PDF 生成器
 *
 * <p>模板文件：{pdf.template.path}/scard.pdf
 * <p>新增生成器参照此类实现，无需改动其他代码
 */
@Component
@RequiredArgsConstructor
public class SCardPdfGenerator implements PdfGenerator<SCardPdfRequest> {

    private final PdfTemplateEngine engine;

    @Override
    public String type() {
        return "s-card";
    }

    @Override
    public Class<SCardPdfRequest> requestType() {
        return SCardPdfRequest.class;
    }

    @Override
    public byte[] generate(SCardPdfRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("qu",         safeStr(req.getQu()));
        fields.put("schoolName", safeStr(req.getSchoolName()));
        fields.put("nationalId", safeStr(req.getNationalId()));
        fields.put("name",       safeStr(req.getName()));
        fields.put("eduId",      safeStr(req.getEduId()));
        fields.put("levelName",  safeStr(req.getLevelName()));

        return engine.fill("s-card", fields);
    }

    private String safeStr(Object value) {
        return Objects.toString(value, "");
    }
}
