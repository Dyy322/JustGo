package com.dyu.justgobackend.dto.response.user;

public record UserFollowRelationResponse(
        boolean following,
        boolean followsYou
) {
}
