package com.yuluo.eyaicodemother.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.routing-chat-model")
@Data
public class RoutingAiModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Boolean logRequests;

    private Boolean logResponses;

    /**
     * 创建用于路由判断的 ChatModel
     */
    @Bean
    @Scope("prototype")
    public ChatModel routingChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
