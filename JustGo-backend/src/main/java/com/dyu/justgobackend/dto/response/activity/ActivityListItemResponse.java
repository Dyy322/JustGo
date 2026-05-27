package com.dyu.justgobackend.dto.response.activity;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityListItemResponse(
        Long id,
        String title,
        String coverImage,
        String locationName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer maxParticipants,
        Integer currentParticipants,
        Integer status,
        Long categoryId,
        String categoryName,
        List<String> tags,
        CreatorInfo creator,
        LocalDateTime createdAt
) {
    public record CreatorInfo(Long id, String nickname, String avatar) {}
}
