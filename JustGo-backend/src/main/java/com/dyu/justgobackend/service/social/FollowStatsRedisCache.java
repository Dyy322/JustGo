package com.dyu.justgobackend.service.social;

import com.dyu.justgobackend.dto.response.user.UserFollowStatsResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 关注/粉丝计数的读缓存，减轻热点用户 stats 行锁竞争；变更关系后主动失效。
 */
@Component
public class FollowStatsRedisCache {
    private static final String KEY_PREFIX = "jg:social:stats:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String FIELD_FOLLOWING = "following";
    private static final String FIELD_FOLLOWERS = "followers";

    private final StringRedisTemplate stringRedisTemplate;

    public FollowStatsRedisCache(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Optional<UserFollowStatsResponse> get(Long userId) {
        String key = KEY_PREFIX + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        try {
            long following = Long.parseLong(String.valueOf(entries.get(FIELD_FOLLOWING)));
            long followers = Long.parseLong(String.valueOf(entries.get(FIELD_FOLLOWERS)));
            return Optional.of(new UserFollowStatsResponse(following, followers));
        } catch (NumberFormatException ex) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public void put(Long userId, UserFollowStatsResponse stats) {
        String key = KEY_PREFIX + userId;
        stringRedisTemplate.opsForHash().putAll(key, Map.of(
                FIELD_FOLLOWING, Long.toString(stats.followingCount()),
                FIELD_FOLLOWERS, Long.toString(stats.followerCount())
        ));
        stringRedisTemplate.expire(key, TTL);
    }

    public void evict(Long userId) {
        stringRedisTemplate.delete(KEY_PREFIX + userId);
    }

    public void evictPair(Long userIdA, Long userIdB) {
        evict(userIdA);
        evict(userIdB);
    }
}
