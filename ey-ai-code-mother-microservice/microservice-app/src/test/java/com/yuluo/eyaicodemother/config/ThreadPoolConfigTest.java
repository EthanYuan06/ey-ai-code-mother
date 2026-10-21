package com.yuluo.eyaicodemother.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证图片收集线程池配置：任务运行在自定义命名线程上，且关键参数正确。
 */
class ThreadPoolConfigTest {

    @Test
    void imageCollectorExecutor_shouldRunOnNamedThreadWithExpectedConfig() {
        ThreadPoolTaskExecutor executor = new ThreadPoolConfig().imageCollectorExecutor();
        // 单元测试中手动初始化；Spring 容器里由生命周期自动完成
        executor.initialize();
        try {
            // 模拟节点中的用法：supplyAsync(supplier, executor)
            String threadName = CompletableFuture
                    .supplyAsync(() -> Thread.currentThread().getName(), executor)
                    .join();

            // 核心断言：任务运行在自定义线程池线程上，而非 ForkJoinPool.commonPool-worker-*
            assertThat(threadName).startsWith("image-collector-");

            // 关键参数校验
            ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
            assertThat(pool.getCorePoolSize()).isEqualTo(8);
            assertThat(pool.getMaximumPoolSize()).isEqualTo(16);
            assertThat(pool.getQueue().remainingCapacity()).isEqualTo(100);
            assertThat(pool.getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
