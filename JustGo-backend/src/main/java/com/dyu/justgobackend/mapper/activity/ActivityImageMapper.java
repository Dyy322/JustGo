package com.dyu.justgobackend.mapper.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyu.justgobackend.entity.activity.ActivityImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityImageMapper extends BaseMapper<ActivityImage> {

    List<ActivityImage> selectByActivityId(@Param("activityId") Long activityId);
}
