package com.dyu.justgobackend.oss;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.oss")
public record OssProperties(
        String endpoint,
        String bucketName,
        String accessKeyId,
        String accessKeySecret,
        @DefaultValue("justgo/") String basePath,
        @DefaultValue("300") long urlExpireSeconds) {
}
