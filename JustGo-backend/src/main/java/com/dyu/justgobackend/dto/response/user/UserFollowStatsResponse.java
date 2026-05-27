package com.dyu.justgobackend.dto.response.user;

public record UserFollowStatsResponse(
        long followingCount,
        long followerCount
) {
}
