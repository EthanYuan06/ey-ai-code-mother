package com.yuluo.eyaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yuluo.eyaicodemother.ai.tools.FileWriteTool;
import com.yuluo.eyaicodemother.ai.tools.ToolManager;
import com.yuluo.eyaicodemother.exception.BusinessException;
import com.yuluo.eyaicodemother.exception.ErrorCode;
import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import com.yuluo.eyaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;
    @Resource
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带本地缓存）
     * 兼容单文件、多文件的同步调用逻辑（门面类）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带本地缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        // 根据 appId 获取 AI Service，获取到则返回，获取不到就创建新的实例并存入缓存
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType) {
        log.info("创建 AI 服务实例，appId: {}", appId);
        // 根据 appId 构建独立的会话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据不同的代码生成类型选择不同的模型配置
        return switch (codeGenType){
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    // 如果生成了幻觉工具名，框架会拦截并告知LLM不存在该工具，让LLM再请求工具调用
                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(
                            toolExecutionRequest,
                                    "Error: there is no tool calling " + toolExecutionRequest.name()
                    ))
                    .build();
            default ->
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持的生成类型：" + codeGenType.getValue());
        };
    }

    /**
     * 默认提供一个 AI Service Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        // 兼容不需要 appId 的场景
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 创建 AI 生成代码类型智能路由实例
     */
    @Bean
    public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }
}
