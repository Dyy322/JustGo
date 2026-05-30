package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.request.activity.ActivityPageQuery;
import com.dyu.justgobackend.dto.request.activity.AddActivityImageRequest;
import com.dyu.justgobackend.dto.request.activity.CreateActivityRequest;
import com.dyu.justgobackend.dto.request.activity.UpdateActivityRequest;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityListItemResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityPageResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse.ImageInfo;

import java.util.List;

public interface ActivityService {

    ActivityDetailResponse create(CreateActivityRequest request);

    ActivityPageResponse<ActivityListItemResponse> list(ActivityPageQuery query);

    ActivityDetailResponse detail(Long activityId);

    ActivityDetailResponse update(Long activityId, UpdateActivityRequest request);

    void cancel(Long activityId);

    void republish(Long activityId);

    void join(Long activityId);

    void leave(Long activityId);

    boolean isJoined(Long activityId);

    ActivityPageResponse<ActivityListItemResponse> myActivities(String type, int page, int size);

    List<ImageInfo> listImages(Long activityId);

    List<ImageInfo> addImages(Long activityId, List<AddActivityImageRequest> requests);

    void deleteImage(Long activityId, Long imageId);
}
