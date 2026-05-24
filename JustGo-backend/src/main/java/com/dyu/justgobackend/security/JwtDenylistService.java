package com.dyu.justgobackend.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 通过 Redis 记录已吊销的 JWT（以 jti 为键），TTL 不大于令牌自然剩余寿命，避免无谓占用内存。
 */
@Service
public class JwtDenylistService {
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public JwtDenylistService(StringRedisTemplate stringRedisTemplate, JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    /** 是否已被登出吊销（尚在 Redis 有效期内）。 */
    public boolean isDenied(String jti) {
        Boolean exists = stringRedisTemplate.hasKey(key(jti));
        return Boolean.TRUE.equals(exists);
    }

    /** 写入黑名单；TTL 通常为令牌剩余有效期。 */
    public void denyUntilExpiry(String jti, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key(jti), "1", ttl);
    }

    private String key(String jti) {
        return jwtProperties.denylistKeyPrefix() + jti;
    }
}
