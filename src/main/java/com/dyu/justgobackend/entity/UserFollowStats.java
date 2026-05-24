package com.dyu.justgobackend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_follow_stats")
public class UserFollowStats {
    @TableId("user_id")
    private Long userId;

    @TableField("following_count")
    private Long followingCount;

    @TableField("follower_count")
    private Long followerCount;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
