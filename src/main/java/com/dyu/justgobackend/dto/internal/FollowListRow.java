package com.dyu.justgobackend.dto.internal;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * MyBatis 映射：关注列表单行（粉丝列表 / 关注列表公用）。
 */
@Data
public class FollowListRow {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDateTime followedAt;
}
