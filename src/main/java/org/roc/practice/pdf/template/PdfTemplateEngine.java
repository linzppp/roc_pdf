package org.roc.practice.pdf.template;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * openPDF AcroForm 模板填写引擎
 *
 * <p>每次调用都重新读取文件，支持热更新模板（无需重启）。
 * PdfReader 非线程安全，不做全局缓存。
 *
 * <p>中文字体：将字体文件（如 NotoSansSC-Regular.ttf）放入
 * {@code resources/fonts/} 目录，打包后从 classpath 加载。
 */
@Slf4j
@Component
public class PdfTemplateEngine {

    private static final String FONT_RESOURCE = "/fonts/NotoSerifCJKsc-VF.ttf";

    @Value("${pdf.template.path}")
    private String templatePath;

    /**
     * 填写模板并返回 PDF 字节数组
     *
     * @param templateName 模板名称（不含扩展名），对应 templatePath 下的 {name}.pdf
     * @param fields       字段名 → 字段值
     * @return 已填写并扁平化（不可编辑）的 PDF 字节
     */
    public byte[] fill(String templateName, Map<String, String> fields) {
        Path templateFile = Paths.get(templatePath, templateName + ".pdf");
        try (InputStream is = Files.newInputStream(templateFile)) {
            return doFill(templateName, is, fields);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template: " + templateFile, e);
        }
    }

    private byte[] doFill(String templateName, InputStream templateStream, Map<String, String> fields) {
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            reader = new PdfReader(templateStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stamper = new PdfStamper(reader, baos);

            AcroFields acroFields = stamper.getAcroFields();

            loadChineseFont(acroFields);

            int successCount = 0, missingCount = 0;
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                try {
                    acroFields.setField(entry.getKey(), entry.getValue());
//                    boolean ok = acroFields.setField(entry.getKey(), entry.getValue());
//                    if (ok) {
//                        successCount++;
//                        log.debug("[PDF-DIAG] setField OK: '{}' = '{}'", entry.getKey(), entry.getValue());
//                    } else {
//                        missingCount++;
//                        log.warn("[PDF-DIAG] setField MISS (not in template): '{}'", entry.getKey());
//                    }
                } catch (Exception e) {
                    log.warn("[PDF-DIAG] setField ERROR: '{}' -> {}", entry.getKey(), e.getMessage());
                }
            }
//            log.info("[PDF-DIAG] setField summary: {} success, {} missing", successCount, missingCount);

            // 扁平化：锁定字段，不允许调用方再次编辑
            stamper.setFormFlattening(true);
            stamper.close();
            stamper = null;

            log.info("[PDF-DIAG] PDF generation complete, size={} bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF fill failed", e);
        } finally {
            if (stamper != null) {
                try { stamper.close(); } catch (Exception ignored) {}
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * 加载中文字体，解决中文乱码问题
     * 字体文件需放在 resources/fonts/ 目录下
     */
    private void loadChineseFont(AcroFields acroFields) {
        try (InputStream fontStream = PdfTemplateEngine.class.getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream == null) {
                log.warn("[PDF-DIAG] Chinese font NOT found at classpath:{} — copy fonts/NotoSerifCJKsc-VF.ttf to src/main/resources/fonts/",
                        FONT_RESOURCE);
                return;
            }
            byte[] fontBytes = fontStream.readAllBytes();
            BaseFont bf = BaseFont.createFont(
                    "NotoSerifCJKsc-VF.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontBytes,
                    null
            );
            acroFields.addSubstitutionFont(bf);
            log.info("[PDF-DIAG] Chinese font loaded: resource={}, bytes={}", FONT_RESOURCE, fontBytes.length);
        } catch (Exception e) {
            log.warn("Failed to load Chinese font: {}", e.getMessage());
        }
    }
}
