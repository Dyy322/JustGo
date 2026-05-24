package com.dyu.justgobackend.service;

import com.dyu.justgobackend.dto.request.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.FollowCursorPageResponse;
import com.dyu.justgobackend.dto.response.FollowUserItemResponse;
import com.dyu.justgobackend.dto.response.UserFollowRelationResponse;
import com.dyu.justgobackend.dto.response.UserFollowStatsResponse;
import com.dyu.justgobackend.dto.response.UserProfileResponse;
import com.dyu.justgobackend.dto.response.UserPublicProfileResponse;

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
