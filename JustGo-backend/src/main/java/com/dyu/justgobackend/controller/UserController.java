package com.dyu.justgobackend.controller;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.dto.request.user.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.user.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.user.FollowCursorPageResponse;
import com.dyu.justgobackend.dto.response.user.FollowUserItemResponse;
import com.dyu.justgobackend.dto.response.user.UserFollowRelationResponse;
import com.dyu.justgobackend.dto.response.user.UserFollowStatsResponse;
import com.dyu.justgobackend.dto.response.user.UserProfileResponse;
import com.dyu.justgobackend.dto.response.user.UserPublicProfileResponse;
import com.dyu.justgobackend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 当前登录用户完整资料（含邮箱、手机号等，仅本人）。 */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.success(userService.getCurrentUserProfile());
    }

    /** 部分更新当前用户资料。 */
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> patchMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(request));
    }

    /** 修改当前用户登录密码。 */
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.success(null);
    }

    /** 根据用户 ID 查看对外公开资料（不含邮箱、手机号）。 */
    @GetMapping("/{id}")
    public ApiResponse<UserPublicProfileResponse> publicProfile(@PathVariable @Positive Long id) {
        return ApiResponse.success(userService.getPublicProfile(id));
    }

    /** 关注指定用户（幂等）。 */
    @PostMapping("/{id}/follow")
    public ApiResponse<Void> follow(@PathVariable @Positive Long id) {
        userService.follow(id);
        return ApiResponse.success(null);
    }

    /** 取消关注（幂等）。 */
    @DeleteMapping("/{id}/follow")
    public ApiResponse<Void> unfollow(@PathVariable @Positive Long id) {
        userService.unfollow(id);
        return ApiResponse.success(null);
    }

    /** 用户关注/粉丝计数（读多写少场景配合 Redis 缓存）。 */
    @GetMapping("/{id}/follow-stats")
    public ApiResponse<UserFollowStatsResponse> followStats(@PathVariable @Positive Long id) {
        return ApiResponse.success(userService.getFollowStats(id));
    }

    /** 当前登录用户与目标用户的双向关注状态。 */
    @GetMapping("/{id}/follow-relation")
    public ApiResponse<UserFollowRelationResponse> followRelation(@PathVariable @Positive Long id) {
        return ApiResponse.success(userService.getFollowRelation(id));
    }

    /** 粉丝列表（按关注时间倒序，cursor 分页）。 */
    @GetMapping("/{id}/followers")
    public ApiResponse<FollowCursorPageResponse<FollowUserItemResponse>> followers(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return ApiResponse.success(userService.listFollowers(id, cursor, limit));
    }

    /** 正在关注列表（按关注时间倒序，cursor 分页）。 */
    @GetMapping("/{id}/following")
    public ApiResponse<FollowCursorPageResponse<FollowUserItemResponse>> following(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return ApiResponse.success(userService.listFollowing(id, cursor, limit));
    }
}
