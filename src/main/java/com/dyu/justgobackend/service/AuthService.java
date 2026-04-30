package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.request.LoginRequest;
import com.dyu.justgobackend.dto.request.RegisterRequest;
import com.dyu.justgobackend.dto.response.LoginResponse;
import com.dyu.justgobackend.dto.response.UserProfileResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String clientIp);

    UserProfileResponse register(RegisterRequest request);

    UserProfileResponse currentUserProfile();

    void logout(String authorizationHeader);
}

