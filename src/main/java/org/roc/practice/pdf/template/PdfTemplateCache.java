package org.roc.practice.pdf.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PDF 模板字节缓存（冷加载）
 *
 * <p>首次访问时从磁盘读取模板文件字节并缓存在内存中，后续请求直接从内存返回。
 * 缓存原始 byte[]（非 PdfReader）：PdfReader 非线程安全，每次 fill 仍需新建实例，
 * 但避免了重复磁盘 I/O。
 */
@Slf4j
@Component
public class PdfTemplateCache {

    @Value("${pdf.template.path}")
    private String templatePath;

    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    /**
     * 获取模板字节，首次调用从磁盘加载并缓存，后续从内存返回。
     */
    public byte[] get(String templateName) {
        return cache.computeIfAbsent(templateName, this::loadFromDisk);
    }

    private byte[] loadFromDisk(String templateName) {
        Path path = Paths.get(templatePath, templateName + ".pdf");
        try {
            byte[] bytes = Files.readAllBytes(path);
            log.info("[PDF-CACHE] Loaded template '{}' into cache, {} bytes", templateName, bytes.length);
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + path, e);
        }
    }

    /**
     * 移除单个模板缓存，下次访问时重新从磁盘加载（用于模板热更新场景）。
     */
    public void evict(String templateName) {
        byte[] removed = cache.remove(templateName);
        if (removed != null) {
            log.info("[PDF-CACHE] Evicted template '{}' from cache", templateName);
        }
    }

    /**
     * 清空全部模板缓存。
     */
    public void evictAll() {
        cache.clear();
        log.info("[PDF-CACHE] All template cache entries cleared");
    }
}