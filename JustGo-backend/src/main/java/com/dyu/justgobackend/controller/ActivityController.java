package com.dyu.justgobackend.controller;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.dto.request.activity.ActivityPageQuery;
import com.dyu.justgobackend.dto.request.activity.AddActivityImageRequest;
import com.dyu.justgobackend.dto.request.activity.CreateActivityRequest;
import com.dyu.justgobackend.dto.request.activity.UpdateActivityRequest;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityDetailResponse.ImageInfo;
import com.dyu.justgobackend.dto.response.activity.ActivityListItemResponse;
import com.dyu.justgobackend.dto.response.activity.ActivityPageResponse;
import com.dyu.justgobackend.service.ActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/activities")
    public ApiResponse<ActivityDetailResponse> create(@Valid @RequestBody CreateActivityRequest request) {
        return ApiResponse.success(activityService.create(request));
    }

    @GetMapping("/activities")
    public ApiResponse<ActivityPageResponse<ActivityListItemResponse>> list(@Valid ActivityPageQuery query) {
        return ApiResponse.success(activityService.list(query));
    }

    @GetMapping("/activities/{id}")
    public ApiResponse<ActivityDetailResponse> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(activityService.detail(id));
    }

    @PutMapping("/activities/{id}")
    public ApiResponse<ActivityDetailResponse> update(@PathVariable @Positive Long id,
                                                       @Valid @RequestBody UpdateActivityRequest request) {
        return ApiResponse.success(activityService.update(id, request));
    }

    @PatchMapping("/activities/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable @Positive Long id) {
        activityService.cancel(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/activities/{id}/republish")
    public ApiResponse<Void> republish(@PathVariable @Positive Long id) {
        activityService.republish(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/activities/{id}/join")
    public ApiResponse<Void> join(@PathVariable @Positive Long id) {
        activityService.join(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/activities/{id}/join")
    public ApiResponse<Void> leave(@PathVariable @Positive Long id) {
        activityService.leave(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/activities/{id}/joined")
    public ApiResponse<Boolean> isJoined(@PathVariable @Positive Long id) {
        return ApiResponse.success(activityService.isJoined(id));
    }

    @GetMapping("/users/me/activities")
    public ApiResponse<ActivityPageResponse<ActivityListItemResponse>> myActivities(
            @RequestParam(defaultValue = "created") String type,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        return ApiResponse.success(activityService.myActivities(type, page, size));
    }

    @GetMapping("/activities/{id}/images")
    public ApiResponse<List<ImageInfo>> listImages(@PathVariable @Positive Long id) {
        return ApiResponse.success(activityService.listImages(id));
    }

    @PostMapping("/activities/{id}/images")
    public ApiResponse<List<ImageInfo>> addImages(@PathVariable @Positive Long id,
                                                   @Valid @RequestBody List<AddActivityImageRequest> requests) {
        return ApiResponse.success(activityService.addImages(id, requests));
    }

    @DeleteMapping("/activities/{id}/images/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable @Positive Long id,
                                          @PathVariable @Positive Long imageId) {
        activityService.deleteImage(id, imageId);
        return ApiResponse.success(null);
    }
}
