package org.roc.practice.pdf.generator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动收集所有 PdfGenerator Bean，按 type() 索引。
 * 新增生成器只需实现 PdfGenerator 并注册为 @Component，无需改此类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfGeneratorRegistry {

    private final List<PdfGenerator<?>> generators;

    private Map<String, PdfGenerator<?>> registry;

    @PostConstruct
    public void init() {
        registry = generators.stream()
                .collect(Collectors.toMap(PdfGenerator::type, Function.identity()));
        log.info("Registered PDF generators: {}", registry.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T> PdfGenerator<T> get(String type) {
        return (PdfGenerator<T>) registry.get(type);
    }

    public boolean contains(String type) {
        return registry.containsKey(type);
    }
}
