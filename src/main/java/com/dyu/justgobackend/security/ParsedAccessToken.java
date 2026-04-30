package com.dyu.justgobackend.security;

/** 已通过签名校验的访问令牌声明（不含黑名单状态）。 */
public record ParsedAccessToken(
        LoginUser loginUser,
        String jti,
        long expiresAtEpochSecond
) {
}
