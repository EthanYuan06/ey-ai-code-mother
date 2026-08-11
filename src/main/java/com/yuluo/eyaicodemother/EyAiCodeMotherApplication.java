package com.yuluo.eyaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.yuluo.eyaicodemother.mapper")
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
public class EyAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(EyAiCodeMotherApplication.class, args);
    }

}
