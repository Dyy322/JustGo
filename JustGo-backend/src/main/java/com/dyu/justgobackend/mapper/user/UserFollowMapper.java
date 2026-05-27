package com.dyu.justgobackend.mapper.user;

import com.dyu.justgobackend.dto.internal.FollowListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserFollowMapper {

    int insertIgnore(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int deleteEdge(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    boolean existsEdge(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    List<FollowListRow> selectFollowersPage(
            @Param("followeeId") Long followeeId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorTieBreaker") Long cursorTieBreaker,
            @Param("limit") int limit);

    List<FollowListRow> selectFollowingPage(
            @Param("followerId") Long followerId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorTieBreaker") Long cursorTieBreaker,
            @Param("limit") int limit);
}
