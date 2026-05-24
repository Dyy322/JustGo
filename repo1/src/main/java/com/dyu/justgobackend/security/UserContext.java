package com.dyu.justgobackend.security;

import java.util.Optional;

/**
 * 用户上下文工具类
 * 基于 ThreadLocal 实现当前登录用户信息的线程隔离存储与访问
 */
public final class UserContext {
    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 将当前登录用户信息存入线程上下文
     *
     * @param user 待存储的登录用户对象
     */
    public static void set(LoginUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 从线程上下文中获取当前登录用户信息
     *
     * @return 包含当前用户信息的 Optional 对象，若未登录则返回空
     */
    public static Optional<LoginUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    /**
     * 清除当前线程的用户上下文信息，防止内存泄漏
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
