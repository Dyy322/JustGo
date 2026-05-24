package com.dyu.justgobackend.dto.response;

public record UserFollowStatsResponse(
        long followingCount,
        long followerCount
) {
}
