package com.dyu.justgobackend.controller;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.dto.request.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.UserProfileResponse;
import com.dyu.justgobackend.dto.response.UserPublicProfileResponse;
import com.dyu.justgobackend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
