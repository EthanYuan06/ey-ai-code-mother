package com.yuluo.eyaicodemother.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 图片收集节点专用线程池。
     * 任务均为阻塞式 I/O（Pexels 图片搜索、Undraw 插画搜索、DashScope Logo 生成），
     * 线程数可高于 CPU 核心数；配合 CallerRunsPolicy 保证任务不丢失并形成背压。
     */
    @Bean("imageCollectorExecutor")
    public ThreadPoolTaskExecutor imageCollectorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("image-collector-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }
}
