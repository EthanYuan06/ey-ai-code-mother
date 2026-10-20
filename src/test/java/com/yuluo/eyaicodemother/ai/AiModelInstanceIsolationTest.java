package com.yuluo.eyaicodemother.ai;

import com.yuluo.eyaicodemother.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 模型实例并发隔离测试
 * 验证 Spring prototype 作用域在并发场景下能够正确隔离 AI 模型实例
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
        // AI 模型配置（测试用占位值，不发起真实请求）
        "DEEPSEEK_BASE_URL=https://api.example.com",
        "DEEPSEEK_API_KEY=test-key",
        "DASHSCOPE_BASE_URL=https://dashscope.example.com",
        "DASHSCOPE_API_KEY=test-key",
        // COS 配置（占位值，满足 ConditionalOnProperty 即可）
        "COS_HOST=https://cos.example.com",
        "COS_SECRET_ID=test-secret-id",
        "COS_SECRET_KEY=test-secret-key",
        "COS_REGION=ap-guangzhou",
        "COS_BUCKET_NAME=test-bucket",
        // 图片搜索配置
        "PEXELS_API_KEY=test-key"
})
class AiModelInstanceIsolationTest {

    /**
     * 并发线程数
     */
    private static final int CONCURRENT_THREADS = 20;

    /**
     * 等待超时时间（秒）
     */
    private static final int AWAIT_TIMEOUT_SECONDS = 30;

    /**
     * 测试路由 ChatModel 原型 Bean 的并发隔离性
     */
    @Test
    void testRoutingChatModelPrototypeIsolation() throws InterruptedException {
        Set<Integer> identityHashes = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    ChatModel model = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
                    identityHashes.add(System.identityHashCode(model));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(true, completed, "并发线程未在超时时间内完成");
        assertEquals(CONCURRENT_THREADS, identityHashes.size(),
                "路由 ChatModel prototype Bean 并发隔离率未达到 100%，存在共享实例");
        log.info("路由 ChatModel 并发隔离测试通过，{} 个线程获取到 {} 个独立实例", CONCURRENT_THREADS, identityHashes.size());
    }

    /**
     * 测试常规流式 StreamingChatModel 原型 Bean 的并发隔离性
     */
    @Test
    void testStreamingChatModelPrototypeIsolation() throws InterruptedException {
        Set<Integer> identityHashes = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    StreamingChatModel model = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                    identityHashes.add(System.identityHashCode(model));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(true, completed, "并发线程未在超时时间内完成");
        assertEquals(CONCURRENT_THREADS, identityHashes.size(),
                "常规 StreamingChatModel prototype Bean 并发隔离率未达到 100%，存在共享实例");
        log.info("常规 StreamingChatModel 并发隔离测试通过，{} 个线程获取到 {} 个独立实例", CONCURRENT_THREADS, identityHashes.size());
    }

    /**
     * 测试推理流式 StreamingChatModel 原型 Bean 的并发隔离性
     */
    @Test
    void testReasoningStreamingChatModelPrototypeIsolation() throws InterruptedException {
        Set<Integer> identityHashes = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    StreamingChatModel model = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                    identityHashes.add(System.identityHashCode(model));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertEquals(true, completed, "并发线程未在超时时间内完成");
        assertEquals(CONCURRENT_THREADS, identityHashes.size(),
                "推理 StreamingChatModel prototype Bean 并发隔离率未达到 100%，存在共享实例");
        log.info("推理 StreamingChatModel 并发隔离测试通过，{} 个线程获取到 {} 个独立实例", CONCURRENT_THREADS, identityHashes.size());
    }
}
