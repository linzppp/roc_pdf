package org.roc.practice.pdf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Value("${pdf.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${pdf.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${pdf.async.queue-capacity:200}")
    private int queueCapacity;

    @Value("${pdf.async.thread-name-prefix:pdf-gen-}")
    private String threadNamePrefix;

    @Bean("pdfTaskExecutor")
    public Executor pdfTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        // 队列满时直接拒绝，由调用方重试，防止线程堆积
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
