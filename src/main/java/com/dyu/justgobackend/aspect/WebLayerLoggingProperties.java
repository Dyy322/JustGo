package com.dyu.justgobackend.aspect;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * 控制 Web 层（{@code @RestController}）访问日志切面的行为。
 */
@ConfigurationProperties(prefix = "app.observability.web-log")
public record WebLayerLoggingProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("1000") long slowThresholdMs,
        List<String> excludePathPatterns,
        @DefaultValue("true") boolean includeClientIp,
        @DefaultValue("***") String maskPlaceholder
) {
    public WebLayerLoggingProperties {
        excludePathPatterns = excludePathPatterns == null
                ? List.of("/api/health")
                : List.copyOf(excludePathPatterns);
        if (maskPlaceholder == null || maskPlaceholder.isBlank()) {
            maskPlaceholder = "***";
        }
    }
}
