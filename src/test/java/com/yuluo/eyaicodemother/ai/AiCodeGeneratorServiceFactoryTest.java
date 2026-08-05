package com.yuluo.eyaicodemother.ai;

import com.yuluo.eyaicodemother.ai.model.HtmlCodeResult;
import com.yuluo.eyaicodemother.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result =
                aiCodeGeneratorService.
                        generateHtmlCode("做个EthanYuan的工作记录小工具，代码不超过50行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode =
                aiCodeGeneratorService.
                        generateMultiFileCode("做个EthanYuan的工作记录小工具，总代码不超过150行");
        Assertions.assertNotNull(multiFileCode);
    }
}
