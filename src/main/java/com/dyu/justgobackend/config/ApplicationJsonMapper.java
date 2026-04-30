package com.dyu.justgobackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * 与 {@link JacksonConfig} 使用同一套默认规则创建 {@link ObjectMapper}，
 * 供无法注入 Bean 的场景（例如纯静态工具类）复用。
 */
public final class ApplicationJsonMapper {

    private ApplicationJsonMapper() {
    }

    public static ObjectMapper create() {
        return JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
