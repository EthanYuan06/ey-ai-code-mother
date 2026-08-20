package com.yuluo.eyaicodemother.langgraph4j.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImageCollectionServiceTest {

    @Resource
    private ImageCollectionService imageCollectionService;

    @Test
    void testTechWebsiteImageCollection() {
        String result = imageCollectionService.collectImages("创建一个个人简历网站，需要展示Java后端开发工程师的技术栈");
        Assertions.assertNotNull(result);
        System.out.println("收集到的图片: " + result);
    }
}
