package com.yuluo.eyaicodemother.ai;

import com.yuluo.eyaicodemother.config.ReasoningStreamingChatModelConfig;
import com.yuluo.eyaicodemother.config.RoutingAiModelConfig;
import com.yuluo.eyaicodemother.config.StreamingChatModelConfig;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AI 模型路由测试
 * 验证不同网站生成需求的路由结果，以及各链路使用的模型
 */
@Slf4j
@SpringBootTest
public class AiModelRoutingTest {

    @Resource
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Resource
    private RoutingAiModelConfig routingAiModelConfig;

    @Resource
    private StreamingChatModelConfig streamingChatModelConfig;

    @Resource
    private ReasoningStreamingChatModelConfig reasoningStreamingChatModelConfig;

    /**
     * 测试三个不同的网站生成需求分别路由到什么类型，并输出各链路使用的模型
     */
    @Test
    void testRoutingWithModelInfo() {
        // ========== 1. 输出各链路使用的模型配置 ==========
        log.info("============ 模型配置信息 ============");
        log.info("[路由] 使用模型: {}", routingAiModelConfig.getModelName());
        log.info("[HTML/MULTI_FILE 流式生成] 使用模型: {}", streamingChatModelConfig.getModelName());
        log.info("[Vue 项目生成] 使用模型: {}", reasoningStreamingChatModelConfig.getModelName());
        log.info("======================================");

        // 验证模型配置正确性
        Assertions.assertEquals("qwen3.7-flash", routingAiModelConfig.getModelName(),
                "路由模型应为 qwen3.7-flash");
        Assertions.assertEquals("deepseek-v4-pro", reasoningStreamingChatModelConfig.getModelName(),
                "Vue 项目生成模型应为 deepseek-v4-pro");

        // ========== 2. 测试三个不同的网站生成需求 ==========
        String[] prompts = {
                "帮我生成一个简单的个人介绍页面，包含姓名、联系方式和一段自我介绍",
                "帮我生成一个精美的个人简历网页，要求包含个人简介、作品展示、博客精选等部分，科技风，可以增加动画效果美化观感",
                "帮我生成一个学生后台管理系统网站，具备对学生信息增删改查的功能，并且查看学生信息需要跳转到新页面"
        };
        String[] expectedTypes = {"HTML", "MULTI_FILE", "VUE_PROJECT"};

        for (int i = 0; i < prompts.length; i++) {
            String prompt = prompts[i];
            // 每次创建独立的路由服务实例（多例模式），模拟真实调用
            AiCodeGenTypeRoutingService routingService = routingServiceFactory.createAiCodeGenTypeRoutingService();
            CodeGenTypeEnum result = routingService.routeCodeGenType(prompt);

            log.info("---- 测试用例 {} ----", i + 1);
            log.info("需求: {}", prompt);
            log.info("路由结果: {} ({})", result.name(), result.getText());
            log.info("路由使用模型: {}", routingAiModelConfig.getModelName());

            // 根据路由结果输出对应的生成模型
            if (result == CodeGenTypeEnum.VUE_PROJECT) {
                log.info("生成使用模型: {} (推理流式模型)", reasoningStreamingChatModelConfig.getModelName());
            } else {
                log.info("生成使用模型: {} (常规流式模型)", streamingChatModelConfig.getModelName());
            }

            Assertions.assertEquals(expectedTypes[i], result.name(),
                    String.format("需求 [%s] 应路由到 %s，实际路由到 %s", prompt, expectedTypes[i], result.name()));
        }

        log.info("============ 所有路由测试通过 ============");
    }
}
