package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.request.LoginRequest;
import com.dyu.justgobackend.dto.request.RefreshTokenRequest;
import com.dyu.justgobackend.dto.request.RegisterRequest;
import com.dyu.justgobackend.dto.response.LoginResponse;
import com.dyu.justgobackend.dto.response.UserProfileResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String clientIp);

    /** 使用 refresh token 轮换访问令牌：旧 refresh 的 jti 加入黑名单直至其自然过期。 */
    LoginResponse refresh(RefreshTokenRequest request);

    UserProfileResponse register(RegisterRequest request);

    UserProfileResponse currentUserProfile();

    void logout(String authorizationHeader);
}

