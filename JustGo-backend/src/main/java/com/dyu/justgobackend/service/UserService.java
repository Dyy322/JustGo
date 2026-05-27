package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.request.user.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.user.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.user.FollowCursorPageResponse;
import com.dyu.justgobackend.dto.response.user.FollowUserItemResponse;
import com.dyu.justgobackend.dto.response.user.UserFollowRelationResponse;
import com.dyu.justgobackend.dto.response.user.UserFollowStatsResponse;
import com.dyu.justgobackend.dto.response.user.UserProfileResponse;
import com.dyu.justgobackend.dto.response.user.UserPublicProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    UserPublicProfileResponse getPublicProfile(Long userId);

    void follow(Long targetUserId);

    void unfollow(Long targetUserId);

    UserFollowStatsResponse getFollowStats(Long userId);

    UserFollowRelationResponse getFollowRelation(Long targetUserId);

    FollowCursorPageResponse<FollowUserItemResponse> listFollowers(Long userId, String cursor, int limit);

    FollowCursorPageResponse<FollowUserItemResponse> listFollowing(Long userId, String cursor, int limit);
}
