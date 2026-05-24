package com.dyu.justgobackend.enums;

/**
 * JWT 载荷中的 token 类型（对应 claim {@code token_use}）。
 */
public enum TokenKind {
    ACCESS("access", "登录凭证已过期"),
    REFRESH("refresh", "刷新凭证已过期，请重新登录");

    private final String claimValue;
    private final String expiredMessage;

    TokenKind(String claimValue, String expiredMessage) {
        this.claimValue = claimValue;
        this.expiredMessage = expiredMessage;
    }

    public String claimValue() {
        return claimValue;
    }

    public String expiredMessage() {
        return expiredMessage;
    }
}
