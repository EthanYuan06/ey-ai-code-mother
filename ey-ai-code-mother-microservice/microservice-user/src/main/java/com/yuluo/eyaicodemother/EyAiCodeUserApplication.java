package com.yuluo.eyaicodemother;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.yuluo.eyaicodemother.mapper")
@ComponentScan("com.yuluo")
@EnableDubbo
public class EyAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(EyAiCodeUserApplication.class, args);
    }
}
