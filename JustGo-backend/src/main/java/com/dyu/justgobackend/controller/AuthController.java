package com.dyu.justgobackend.controller;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.config.CookieProperties;
import com.dyu.justgobackend.dto.request.auth.LoginRequest;
import com.dyu.justgobackend.dto.response.auth.LoginResponse;
import com.dyu.justgobackend.dto.request.auth.RefreshTokenRequest;
import com.dyu.justgobackend.dto.request.auth.RegisterRequest;
import com.dyu.justgobackend.dto.response.user.UserProfileResponse;
import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.service.AuthService;
import com.dyu.justgobackend.util.AuthorizationHeaderUtils;
import com.dyu.justgobackend.util.CookieUtils;
import com.dyu.justgobackend.util.HttpServletRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieProperties cookieProperties;

    public AuthController(AuthService authService, CookieProperties cookieProperties) {
        this.authService = authService;
        this.cookieProperties = cookieProperties;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest servletRequest,
                                            HttpServletResponse servletResponse) {
        LoginResponse loginResponse = authService.login(request, HttpServletRequestUtils.clientIp(servletRequest));
        setAuthCookies(servletResponse, loginResponse);
        return ApiResponse.success(loginResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                              HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse) {
        String refreshToken = CookieUtils.extractRefreshToken(servletRequest)
                .or(() -> Optional.ofNullable(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(401, "刷新令牌不存在"));

        LoginResponse loginResponse = authService.refresh(refreshToken);
        setAuthCookies(servletResponse, loginResponse);
        return ApiResponse.success(loginResponse);
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.success(authService.currentUserProfile());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest,
                                    HttpServletResponse servletResponse) {
        String accessToken = CookieUtils.extractAccessToken(servletRequest)
                .or(() -> AuthorizationHeaderUtils.bearerToken(
                        servletRequest.getHeader(HttpHeaders.AUTHORIZATION)))
                .orElseThrow(() -> new BusinessException(401, "请先登录"));

        authService.logout(accessToken);
        CookieUtils.clearAuthCookies(servletResponse, cookieProperties.secure());
        return ApiResponse.success(null);
    }

    private void setAuthCookies(HttpServletResponse response, LoginResponse loginResponse) {
        CookieUtils.setAccessTokenCookie(response, loginResponse.accessToken(),
                loginResponse.expiresIn(), cookieProperties.secure());
        CookieUtils.setRefreshTokenCookie(response, loginResponse.refreshToken(),
                loginResponse.refreshExpiresIn(), cookieProperties.secure());
    }
}
