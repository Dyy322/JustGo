package com.dyu.justgobackend.security;

public record LoginUser(
        Long id,
        String username,
        Integer accountType
) {
}
