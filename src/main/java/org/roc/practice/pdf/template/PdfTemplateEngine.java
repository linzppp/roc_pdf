package org.roc.practice.pdf.template;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * openPDF AcroForm 模板填写引擎
 *
 * <p>模板字节由 {@link PdfTemplateCache} 冷加载并缓存，消除每次请求的磁盘 I/O。
 * PdfReader 非线程安全，仍在每次调用时新建实例。
 *
 * <p>中文字体：将字体文件（如 NotoSansSC-Regular.ttf）放入
 * {@code resources/fonts/} 目录，打包后从 classpath 加载。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTemplateEngine {

    private static final String FONT_RESOURCE = "/fonts/NotoSerifCJKsc-VF.ttf";

    private final PdfTemplateCache templateCache;
    private final PdfTimingRecorder timingRecorder;

    /** 字体在启动时预加载并常驻内存，BaseFont 只读，多线程安全 */
    private BaseFont cachedFont;

    @PostConstruct
    private void initFont() {
        try (InputStream fontStream = PdfTemplateEngine.class.getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream == null) {
                log.warn("[PDF-CACHE] Chinese font NOT found at classpath:{}", FONT_RESOURCE);
                return;
            }
            byte[] fontBytes = fontStream.readAllBytes();
            cachedFont = BaseFont.createFont(
                    "NotoSerifCJKsc-VF.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontBytes,
                    null
            );
            log.info("[PDF-CACHE] Chinese font pre-loaded: {} bytes", fontBytes.length);
        } catch (Exception e) {
            log.warn("[PDF-CACHE] Failed to pre-load Chinese font: {}", e.getMessage());
        }
    }

    /**
     * 填写模板并返回 PDF 字节数组
     *
     * @param templateName 模板名称（不含扩展名），对应 templatePath 下的 {name}.pdf
     * @param fields       字段名 → 字段值
     * @return 已填写并扁平化（不可编辑）的 PDF 字节
     */
    public byte[] fill(String templateName, Map<String, String> fields) {
        long t0 = System.currentTimeMillis();
        byte[] templateBytes = templateCache.get(templateName);
        timingRecorder.record(templateName, PdfTimingStep.TEMPLATE_LOAD, System.currentTimeMillis() - t0);
        return doFill(templateName, templateBytes, fields);
    }

    private byte[] doFill(String templateName, byte[] templateBytes, Map<String, String> fields) {
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            reader = new PdfReader(templateBytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stamper = new PdfStamper(reader, baos);

            AcroFields acroFields = stamper.getAcroFields();

            long t1 = System.currentTimeMillis();
            loadChineseFont(acroFields);
            timingRecorder.record(templateName, PdfTimingStep.FONT_SETUP, System.currentTimeMillis() - t1);

            long t2 = System.currentTimeMillis();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                try {
                    acroFields.setField(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    log.warn("[PDF-DIAG] setField ERROR: '{}' -> {}", entry.getKey(), e.getMessage());
                }
            }
            timingRecorder.record(templateName, PdfTimingStep.FIELD_FILL, System.currentTimeMillis() - t2);

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
     * 将预加载的字体注入 AcroFields，仅做引用传递，无 I/O 或解析开销
     */
    private void loadChineseFont(AcroFields acroFields) {
        if (cachedFont == null) {
            log.warn("[PDF-DIAG] Chinese font not available, skipping substitution");
            return;
        }
        acroFields.addSubstitutionFont(cachedFont);
    }
}
