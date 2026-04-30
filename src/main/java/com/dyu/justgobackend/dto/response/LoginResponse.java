package com.dyu.justgobackend.dto.response;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserProfileResponse user
) {
}
