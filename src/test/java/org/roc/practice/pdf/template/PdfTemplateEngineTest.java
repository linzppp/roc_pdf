package org.roc.practice.pdf.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PdfTemplateEngine 单元测试
 *
 * <p>如需真实测试，将 invoice.pdf 模板放入 src/test/resources/pdf-templates/ 后运行。
 */
class PdfTemplateEngineTest {

    @TempDir
    Path tempDir;

    PdfTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PdfTemplateEngine();
        ReflectionTestUtils.setField(engine, "templatePath", tempDir.toString());
    }

    @Test
    void fill_shouldReturnNonEmptyBytes_whenTemplateExists() throws IOException {
        // 从测试资源加载模板（如不存在则跳过）
        InputStream templateStream = getClass().getResourceAsStream("/pdf-templates/invoice.pdf");
        assumeTrue(templateStream != null, "invoice.pdf template not found in test resources, skipping");

        Path templateFile = tempDir.resolve("invoice.pdf");
        Files.copy(templateStream, templateFile);

        Map<String, String> fields = Map.of(
                "invoiceNo",   "INV-2026-001",
                "invoiceDate", "2026-04-20",
                "buyerName",   "测试公司",
                "totalAmount", "1000.00"
        );

        byte[] result = engine.fill("invoice", fields);

        assertThat(result).isNotEmpty();
        // PDF 文件以 %PDF 开头
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }
}
