package com.dyu.justgobackend.dto.response;

public record UserFollowRelationResponse(
        boolean following,
        boolean followsYou
) {
}
