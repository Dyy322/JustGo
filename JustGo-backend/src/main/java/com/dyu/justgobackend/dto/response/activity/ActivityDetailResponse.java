package com.dyu.justgobackend.dto.response.activity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityDetailResponse(
        Long id,
        String title,
        String description,
        String coverImage,
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer maxParticipants,
        Integer currentParticipants,
        Integer status,
        Long categoryId,
        String categoryName,
        List<ImageInfo> images,
        List<String> tags,
        CreatorInfo creator,
        Long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ImageInfo(Long id, String url, Integer sortOrder) {}
    public record CreatorInfo(Long id, String nickname, String avatar) {}
}
