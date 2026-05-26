package com.dyu.justgobackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        @DefaultValue("false") boolean secure) {
}
