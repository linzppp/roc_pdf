package org.roc.practice.pdf.api.controller;

import org.roc.practice.pdf.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JVM 监控实验接口
 *
 * <p>正常回收场景：
 * <pre>
 * POST /jvm/heap/allocate?sizeMB=10&count=5      堆内存分配，不持有引用，可被 GC 回收
 * POST /jvm/direct/allocate?sizeMB=10&count=5     DirectMemory 分配后显式释放
 * </pre>
 *
 * <p>内存泄漏场景：
 * <pre>
 * POST /jvm/heap/leak?sizeMB=10&count=5           堆内存泄漏，对象存入 static List 永不释放
 * POST /jvm/direct/leak?sizeMB=10&count=5         DirectMemory 泄漏，ByteBuffer 存入 static List 永不释放
 * </pre>
 *
 * <p>辅助：
 * <pre>
 * POST /jvm/leak/clear                             清空泄漏列表，重置实验状态
 * GET  /jvm/leak/stats                             查看当前泄漏量
 * </pre>
 */
@RestController
@RequestMapping("/jvm")
public class JvmLabController {

    /** 堆内存泄漏容器 */
    private static final List<byte[]> HEAP_LEAK = new ArrayList<>();
    /** DirectMemory 泄漏容器 */
    private static final List<ByteBuffer> DIRECT_LEAK = new ArrayList<>();

    // ==================== 正常回收 ====================

    /**
     * 堆内存分配 —— 分配后不持有引用，可被 GC 正常回收
     */
    @PostMapping("/heap/allocate")
    public R<Map<String, Object>> heapAllocate(
            @RequestParam(defaultValue = "10") int sizeMB,
            @RequestParam(defaultValue = "5") int count) {
        long total = 0;
        for (int i = 0; i < count; i++) {
            byte[] block = new byte[sizeMB * 1024 * 1024];
            total += block.length;
        }
        // block 出了作用域，引用断开，可被 GC 回收
        return R.ok(Map.of(
                "type", "heap",
                "action", "allocate-and-release",
                "totalAllocatedMB", total / 1024 / 1024
        ));
    }

    /**
     * DirectMemory 分配 —— 分配后不持有引用，由 GC 触发 Cleaner 回收堆外内存
     */
    @PostMapping("/direct/allocate")
    public R<Map<String, Object>> directAllocate(
            @RequestParam(defaultValue = "10") int sizeMB,
            @RequestParam(defaultValue = "5") int count) {
        long total = 0;
        for (int i = 0; i < count; i++) {
            ByteBuffer buf = ByteBuffer.allocateDirect(sizeMB * 1024 * 1024);
            total += buf.capacity();
            // buf 出作用域后，GC 回收 DirectByteBuffer 对象时会触发 Cleaner 释放堆外内存
        }
        System.gc(); // 提示 JVM 回收，加速 Cleaner 执行
        return R.ok(Map.of(
                "type", "direct",
                "action", "allocate-and-release",
                "totalAllocatedMB", total / 1024 / 1024
        ));
    }

    // ==================== 内存泄漏 ====================

    /**
     * 堆内存泄漏 —— 对象存入 static List，永不释放
     */
    @PostMapping("/heap/leak")
    public R<Map<String, Object>> heapLeak(
            @RequestParam(defaultValue = "10") int sizeMB,
            @RequestParam(defaultValue = "5") int count) {
        long added = 0;
        for (int i = 0; i < count; i++) {
            byte[] block = new byte[sizeMB * 1024 * 1024];
            HEAP_LEAK.add(block);
            added += block.length;
        }
        return R.ok(Map.of(
                "type", "heap",
                "action", "leak",
                "addedMB", added / 1024 / 1024,
                "totalLeakedMB", HEAP_LEAK.stream().mapToLong(b -> b.length).sum() / 1024 / 1024
        ));
    }

    /**
     * DirectMemory 泄漏 —— ByteBuffer 存入 static List，永不释放
     */
    @PostMapping("/direct/leak")
    public R<Map<String, Object>> directLeak(
            @RequestParam(defaultValue = "10") int sizeMB,
            @RequestParam(defaultValue = "5") int count) {
        long added = 0;
        for (int i = 0; i < count; i++) {
            ByteBuffer buf = ByteBuffer.allocateDirect(sizeMB * 1024 * 1024);
            DIRECT_LEAK.add(buf);
            added += buf.capacity();
        }
        return R.ok(Map.of(
                "type", "direct",
                "action", "leak",
                "addedMB", added / 1024 / 1024,
                "totalLeakedMB", DIRECT_LEAK.stream().mapToLong(ByteBuffer::capacity).sum() / 1024 / 1024
        ));
    }

    // ==================== 辅助 ====================

    /**
     * 清空泄漏列表，重置实验状态
     */
    @PostMapping("/leak/clear")
    public R<String> clearLeak() {
        HEAP_LEAK.clear();
        DIRECT_LEAK.clear();
        System.gc();
        return R.ok("leak lists cleared, System.gc() triggered");
    }

    /**
     * 查看当前泄漏量
     */
    @GetMapping("/leak/stats")
    public R<Map<String, Object>> leakStats() {
        return R.ok(Map.of(
                "heapLeakEntries", HEAP_LEAK.size(),
                "heapLeakMB", HEAP_LEAK.stream().mapToLong(b -> b.length).sum() / 1024 / 1024,
                "directLeakEntries", DIRECT_LEAK.size(),
                "directLeakMB", DIRECT_LEAK.stream().mapToLong(ByteBuffer::capacity).sum() / 1024 / 1024
        ));
    }
}