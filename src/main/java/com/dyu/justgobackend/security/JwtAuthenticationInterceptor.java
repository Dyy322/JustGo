package com.dyu.justgobackend.security;

import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.util.AuthorizationHeaderUtils;
import com.dyu.justgobackend.util.HttpServletResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * JWT 认证拦截器
 * 负责在请求到达 Controller 之前校验 Token 有效性，并维护用户上下文
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtDenylistService jwtDenylistService;

    public JwtAuthenticationInterceptor(JwtTokenProvider jwtTokenProvider, JwtDenylistService jwtDenylistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtDenylistService = jwtDenylistService;
    }

    /**
     * 预处理请求：校验 JWT Token 并将登录用户存入 ThreadLocal
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 目标处理器
     * @return true 表示放行，false 表示中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<String> bearerToken = AuthorizationHeaderUtils.bearerToken(authorization);
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