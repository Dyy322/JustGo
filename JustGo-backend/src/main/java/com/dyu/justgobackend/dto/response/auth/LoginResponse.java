package com.dyu.justgobackend.dto.response.auth;

import com.dyu.justgobackend.dto.response.user.UserProfileResponse;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        UserProfileResponse user
) {
}
