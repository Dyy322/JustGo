package com.dyu.justgobackend.dto.response.user;

import com.dyu.justgobackend.entity.user.User;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String phone,
        String nickname,
        String avatar,
        Integer gender,
        Integer status,
        Integer accountType
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatar(),
                user.getGender(),
                user.getStatus(),
                user.getAccountType()
        );
    }
}
