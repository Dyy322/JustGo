package com.dyu.justgobackend.mapper.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyu.justgobackend.dto.response.activity.ActivityListItemResponse;
import com.dyu.justgobackend.entity.activity.Activity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityMapper extends BaseMapper<Activity> {

    List<ActivityListItemResponse> selectPageWithDetails(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    long countPage(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);

    Activity selectDetailById(@Param("id") Long id);

    int updateStatusByTime(@Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus,
            @Param("now") LocalDateTime now);

    int updateEndedStatus(@Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus,
            @Param("now") LocalDateTime now);

    List<String> selectTagsByActivityId(@Param("activityId") Long activityId);
}
