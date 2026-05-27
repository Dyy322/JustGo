package com.dyu.justgobackend.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dyu.justgobackend.exception.BusinessException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * 关注列表游标：Base64URL(JSON) ，字段 t=created_at 毫秒，u=tie-breaker 用户 ID。
 */
@Component
public class FollowCursorCodec {
    private final ObjectMapper objectMapper;

    public FollowCursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<DecodedFollowCursor> decode(@Nullable String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return Optional.empty();
        }
        try {
            byte[] raw = Base64.getUrlDecoder().decode(cursor);
            JsonNode node = objectMapper.readTree(raw);
            long epochMillis = node.get("t").asLong();
            long tieBreakerUserId = node.get("u").asLong();
            LocalDateTime createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            return Optional.of(new DecodedFollowCursor(createdAt, tieBreakerUserId));
        } catch (Exception e) {
            throw new BusinessException(400, "cursor 无效");
        }
    }

    public String encode(LocalDateTime createdAt, long tieBreakerUserId) {
        try {
            long millis = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            byte[] raw = objectMapper.writeValueAsBytes(Map.of("t", millis, "u", tieBreakerUserId));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new BusinessException(500, "cursor 生成失败");
        }
    }

    public record DecodedFollowCursor(LocalDateTime createdAt, long tieBreakerUserId) {
    }
}
