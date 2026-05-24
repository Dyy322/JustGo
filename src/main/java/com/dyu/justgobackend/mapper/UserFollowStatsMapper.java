package com.dyu.justgobackend.mapper;

import com.dyu.justgobackend.entity.UserFollowStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFollowStatsMapper {

    int ensureRow(@Param("userId") Long userId);

    UserFollowStats selectByUserId(@Param("userId") Long userId);

    int incrementFollowing(@Param("userId") Long userId);

    int incrementFollowers(@Param("userId") Long userId);

    int decrementFollowing(@Param("userId") Long userId);

    int decrementFollowers(@Param("userId") Long userId);
}
