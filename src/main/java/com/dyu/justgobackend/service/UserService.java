package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.request.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.UserProfileResponse;
import com.dyu.justgobackend.dto.response.UserPublicProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    UserPublicProfileResponse getPublicProfile(Long userId);
}
