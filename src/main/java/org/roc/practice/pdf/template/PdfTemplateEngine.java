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

    /**
     * 字体字节在启动时从 classpath 加载并常驻内存。
     * BaseFont 实例通过 ThreadLocal 缓存：每线程解析一次字体文件，后续复用同一实例。
     * 既避免每次请求重复解析 CJK 大字体（CPU 密集型），又因实例不跨线程共享，
     * 消除 TrueTypeFontUnicode.convertToBytes 的 synchronized 跨线程竞争。
     */
    private byte[] cachedFontBytes;

    private final ThreadLocal<BaseFont> threadLocalFont = ThreadLocal.withInitial(() -> {
        if (cachedFontBytes == null) {
            return null;
        }
        try {
            return BaseFont.createFont(
                    "NotoSerifCJKsc-VF.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    false,
                    cachedFontBytes,
                    null
            );
        } catch (Exception e) {
            log.warn("[PDF-DIAG] Failed to create thread-local font: {}", e.getMessage());
            return null;
        }
    });

    @PostConstruct
    private void initFont() {
        try (InputStream fontStream = PdfTemplateEngine.class.getResourceAsStream(FONT_RESOURCE)) {
            if (fontStream == null) {
                log.warn("[PDF-CACHE] Chinese font NOT found at classpath:{}", FONT_RESOURCE);
                return;
            }
            cachedFontBytes = fontStream.readAllBytes();
            log.info("[PDF-CACHE] Chinese font bytes pre-loaded: {} bytes", cachedFontBytes.length);
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
        byte[] templateBytes = templateCache.get(templateName);
        return doFill(templateBytes, fields);
    }

    private byte[] doFill(byte[] templateBytes, Map<String, String> fields) {
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            reader = new PdfReader(templateBytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stamper = new PdfStamper(reader, baos);

            AcroFields acroFields = stamper.getAcroFields();
            loadChineseFont(acroFields);

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                try {
                    acroFields.setField(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    log.warn("[PDF-DIAG] setField ERROR: '{}' -> {}", entry.getKey(), e.getMessage());
                }
            }

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
     * 从 ThreadLocal 缓存获取 BaseFont 实例并注入 AcroFields。
     * 每线程首次调用时解析字体文件，后续请求复用同一实例，消除重复解析开销。
     * 实例不跨线程共享，TrueTypeFontUnicode.convertToBytes 的 synchronized 不产生竞争。
     */
    private void loadChineseFont(AcroFields acroFields) {
        BaseFont bf = threadLocalFont.get();
        if (bf == null) {
            log.warn("[PDF-DIAG] Chinese font not available, skipping substitution");
            return;
        }
        acroFields.addSubstitutionFont(bf);
    }
}
