package com.dyu.justgobackend.dto.response.user;

import com.dyu.justgobackend.entity.user.User;

/**
 * 对外展示的用户资料（不含邮箱、手机号等敏感字段）。
 */
public record UserPublicProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatar,
        Integer gender
) {
    public static UserPublicProfileResponse from(User user) {
        return new UserPublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getGender()
        );
    }
}
