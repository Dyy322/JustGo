package com.dyu.justgobackend.security;

import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.util.AuthorizationHeaderUtils;
import com.dyu.justgobackend.util.CookieUtils;
import com.dyu.justgobackend.util.HttpServletResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * JWT 认证拦截器
 * 优先从 httpOnly Cookie 读取令牌，兜底从 Authorization 头读取（兼容移动端）。
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtDenylistService jwtDenylistService;

    public JwtAuthenticationInterceptor(JwtTokenProvider jwtTokenProvider, JwtDenylistService jwtDenylistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtDenylistService = jwtDenylistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Optional<String> bearerToken = extractToken(request);
        if (bearerToken.isEmpty()) {
            HttpServletResponseUtils.writeUnauthorized(response, "请先登录");
            return false;
        }

        try {
            ParsedToken accessToken = jwtTokenProvider.parseAccessToken(bearerToken.get());
            if (jwtDenylistService.isDenied(accessToken.jti())) {
                HttpServletResponseUtils.writeUnauthorized(response, "登录已失效，请重新登录");
                return false;
            }
            UserContext.set(accessToken.loginUser());
            return true;
        } catch (BusinessException exception) {
            HttpServletResponseUtils.writeUnauthorized(response, exception.getMessage());
            return false;
        }
    }

    /** 优先从 httpOnly Cookie 读取，兜底从 Authorization 头读取（兼容移动端）。 */
    private Optional<String> extractToken(HttpServletRequest request) {
        Optional<String> cookieToken = CookieUtils.extractAccessToken(request);
        if (cookieToken.isPresent()) {
            return cookieToken;
        }
        return AuthorizationHeaderUtils.bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    /**
     * 请求完成后清理线程上下文，防止内存泄漏
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 目标处理器
     * @param ex 处理过程中产生的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}