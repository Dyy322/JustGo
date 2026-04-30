package com.dyu.justgobackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson JSON 序列化配置类
 * 用于定制全局 ObjectMapper 的行为，统一项目的 JSON 处理规范
 */
@Configuration
public class JacksonConfig {
    /**
     * 创建并配置全局 ObjectMapper Bean
     * 自动注册 Java 时间模块，并将日期序列化为 ISO-8601 格式字符串
     *
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
