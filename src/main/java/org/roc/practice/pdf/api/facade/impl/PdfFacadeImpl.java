package org.roc.practice.pdf.api.facade.impl;

import lombok.RequiredArgsConstructor;
import org.roc.practice.pdf.api.facade.PdfFacade;
import org.roc.practice.pdf.dto.response.PdfTaskResponse;
import org.roc.practice.pdf.service.PdfService;
import org.springframework.stereotype.Component;

/**
 * PdfFacade 实现，薄封装 PdfService。
 * Controller 和未来 RPC Provider 均调用此类，确保逻辑一致。
 */
@Component
@RequiredArgsConstructor
public class PdfFacadeImpl implements PdfFacade {

    private final PdfService pdfService;

    @Override
    public PdfTaskResponse submit(String type, String requestBody, String bizId) {
        return pdfService.submit(type, requestBody, bizId);
    }

    @Override
    public PdfTaskResponse queryTask(String taskId) {
        return pdfService.queryTask(taskId);
    }

    @Override
    public void deleteTask(String taskId) {
        pdfService.deleteTask(taskId);
    }
}
