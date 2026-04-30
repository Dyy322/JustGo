package com.dyu.justgobackend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration expiration,
        /* Redis 中 jti 黑名单键前缀，须与运维侧隔离命名空间。 */
        String denylistKeyPrefix
) {
}
