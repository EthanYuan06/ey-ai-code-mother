package com.yuluo.eyaicodemother.core;

import com.yuluo.eyaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Test
    void generateAndSaveCode() {
        File file =
                aiCodeGeneratorFacade
                        .generateAndSaveCode(
                                "做个EthanYuan的工作记录小工具，代码不超过150行",
                                CodeGenTypeEnum.HTML
                        );
        Assertions.assertNotNull(file);
    }
}