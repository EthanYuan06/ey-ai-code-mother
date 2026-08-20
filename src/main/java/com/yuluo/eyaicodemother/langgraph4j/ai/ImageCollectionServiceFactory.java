package com.yuluo.eyaicodemother.langgraph4j.ai;

import com.yuluo.eyaicodemother.langgraph4j.tools.ImageSearchTool;
import com.yuluo.eyaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.yuluo.eyaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {
    @Resource
    private ChatModel chatModel;
    @Resource
    private ImageSearchTool imageSearchTool;
    @Resource
    private LogoGeneratorTool logoGeneratorTool;
    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    /**
     * 创建图片收集AI服务
     * @return AI 服务实例
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(imageSearchTool, logoGeneratorTool, undrawIllustrationTool)
                .chatModel(chatModel)
                .build();
    }
}
