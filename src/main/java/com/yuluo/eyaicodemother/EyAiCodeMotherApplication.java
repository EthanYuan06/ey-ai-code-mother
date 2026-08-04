package com.yuluo.eyaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.yuluo.eyaicodemother.mapper")
public class EyAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(EyAiCodeMotherApplication.class, args);
    }

}
