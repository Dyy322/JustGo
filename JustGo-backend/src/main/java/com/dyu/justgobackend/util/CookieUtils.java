package com.dyu.justgobackend.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Optional;

public final class CookieUtils {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private CookieUtils() {
    }

    public static void setAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void setRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void clearAuthCookies(HttpServletResponse response, boolean secure) {
        ResponseCookie access = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(0)
                .build();
        ResponseCookie refresh = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", access.toString());
        response.addHeader("Set-Cookie", refresh.toString());
    }

    public static Optional<String> extractAccessToken(HttpServletRequest request) {
        return extractCookie(request, ACCESS_TOKEN_COOKIE);
    }

    public static Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookie(request, REFRESH_TOKEN_COOKIE);
    }

    private static Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst();
    }
}
