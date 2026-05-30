package com.dyu.justgobackend.mapper.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyu.justgobackend.entity.activity.ActivityParticipant;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityParticipantMapper extends BaseMapper<ActivityParticipant> {

    int countByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    List<Long> selectJoinedActivityIds(@Param("userId") Long userId);
}
