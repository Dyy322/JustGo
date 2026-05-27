package com.dyu.justgobackend.mapper.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyu.justgobackend.entity.activity.ActivityTagRel;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.List;

public interface ActivityTagRelMapper extends BaseMapper<ActivityTagRel> {

    int insertBatch(@Param("activityId") Long activityId, @Param("tagIds") List<Long> tagIds);

    List<Map<String, Object>> selectTagNamesByActivityIds(@Param("activityIds") List<Long> activityIds);

    int deleteByActivityId(@Param("activityId") Long activityId);

    int deleteByTagId(@Param("tagId") Long tagId);
}
