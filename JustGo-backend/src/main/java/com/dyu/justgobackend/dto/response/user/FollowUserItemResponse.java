package com.dyu.justgobackend.dto.response.user;

import com.dyu.justgobackend.dto.internal.FollowListRow;

import java.time.LocalDateTime;

public record FollowUserItemResponse(
        Long userId,
        String username,
        String nickname,
        String avatar,
        Integer gender,
        LocalDateTime followedAt
) {
    public static FollowUserItemResponse from(FollowListRow row) {
        return new FollowUserItemResponse(
                row.getUserId(),
                row.getUsername(),
                row.getNickname(),
                row.getAvatar(),
                row.getGender(),
                row.getFollowedAt()
        );
    }
}
