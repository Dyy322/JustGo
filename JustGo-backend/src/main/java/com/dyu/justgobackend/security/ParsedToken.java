package com.dyu.justgobackend.security;

/**
 * 已通过签名校验的 JWT 声明（access 与 refresh 共用结构；不含黑名单状态）。
 */
public record ParsedToken(
        LoginUser loginUser,
        String jti,
        long expiresAtEpochSecond
) {
}
