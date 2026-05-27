package com.dyu.justgobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dyu.justgobackend.dto.internal.FollowListRow;
import com.dyu.justgobackend.dto.request.user.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.user.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.user.FollowCursorPageResponse;
import com.dyu.justgobackend.dto.response.user.FollowUserItemResponse;
import com.dyu.justgobackend.dto.response.user.UserFollowRelationResponse;
import com.dyu.justgobackend.dto.response.user.UserFollowStatsResponse;
import com.dyu.justgobackend.dto.response.user.UserProfileResponse;
import com.dyu.justgobackend.dto.response.user.UserPublicProfileResponse;
import com.dyu.justgobackend.entity.user.User;
import com.dyu.justgobackend.entity.user.UserFollowStats;
import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.mapper.user.UserFollowMapper;
import com.dyu.justgobackend.mapper.user.UserFollowStatsMapper;
import com.dyu.justgobackend.mapper.user.UserMapper;
import com.dyu.justgobackend.security.LoginUser;
import com.dyu.justgobackend.security.UserContext;
import com.dyu.justgobackend.service.UserService;
import com.dyu.justgobackend.service.social.FollowCursorCodec;
import com.dyu.justgobackend.service.social.FollowStatsRedisCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserFollowStatsMapper userFollowStatsMapper;
    private final FollowCursorCodec followCursorCodec;
    private final FollowStatsRedisCache followStatsRedisCache;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserMapper userMapper,
            UserFollowMapper userFollowMapper,
            UserFollowStatsMapper userFollowStatsMapper,
            FollowCursorCodec followCursorCodec,
            FollowStatsRedisCache followStatsRedisCache,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userFollowMapper = userFollowMapper;
        this.userFollowStatsMapper = userFollowStatsMapper;
        this.followCursorCodec = followCursorCodec;
        this.followStatsRedisCache = followStatsRedisCache;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        return UserProfileResponse.from(requireUserByCurrentId());
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        if (!hasAnyProfileField(request)) {
            throw new BusinessException(400, "请至少提交一项要修改的资料");
        }

        Long userId = currentUserId();
        if (StringUtils.hasText(request.email())) {
            User byEmail = userMapper.selectByEmail(request.email());
            if (byEmail != null && !Objects.equals(byEmail.getId(), userId)) {
                throw new BusinessException(400, "该邮箱已被其他账号使用");
            }
        }
        if (StringUtils.hasText(request.phone())) {
            User byPhone = userMapper.selectByPhone(request.phone());
            if (byPhone != null && !Objects.equals(byPhone.getId(), userId)) {
                throw new BusinessException(400, "该手机号已被其他账号使用");
            }
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .isNull(User::getDeletedAt);

        if (StringUtils.hasText(request.nickname())) {
            wrapper.set(User::getNickname, request.nickname());
        }
        if (request.avatar() != null) {
            wrapper.set(User::getAvatar, request.avatar());
        }
        if (request.gender() != null) {
            wrapper.set(User::getGender, request.gender());
        }
        if (request.email() != null) {
            wrapper.set(User::getEmail, StringUtils.hasText(request.email()) ? request.email() : null);
        }
        if (request.phone() != null) {
            wrapper.set(User::getPhone, StringUtils.hasText(request.phone()) ? request.phone() : null);
        }

        int rows = userMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException(404, "用户不存在或已删除");
        }

        return UserProfileResponse.from(requireUserById(userId));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = requireUserByCurrentId();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(400, "新密码不能与当前密码相同");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(request.newPassword()));
        userMapper.update(null, wrapper);
    }

    @Override
    public UserPublicProfileResponse getPublicProfile(Long userId) {
        User user = requireUserById(userId);
        if (!user.isEnabled()) {
            throw new BusinessException(404, "用户不存在");
        }
        return UserPublicProfileResponse.from(user);
    }

    @Override
    @Transactional
    public void follow(Long targetUserId) {
        Long followerId = currentUserId();
        if (Objects.equals(followerId, targetUserId)) {
            throw new BusinessException(400, "不能关注自己");
        }
        requireFollowableUser(targetUserId);

        int inserted = userFollowMapper.insertIgnore(followerId, targetUserId);
        if (inserted == 0) {
            return;
        }
        userFollowStatsMapper.incrementFollowing(followerId);
        userFollowStatsMapper.incrementFollowers(targetUserId);
        followStatsRedisCache.evictPair(followerId, targetUserId);
    }

    @Override
    @Transactional
    public void unfollow(Long targetUserId) {
        Long followerId = currentUserId();
        if (Objects.equals(followerId, targetUserId)) {
            return;
        }

        int deleted = userFollowMapper.deleteEdge(followerId, targetUserId);
        if (deleted == 0) {
            return;
        }
        userFollowStatsMapper.decrementFollowing(followerId);
        userFollowStatsMapper.decrementFollowers(targetUserId);
        followStatsRedisCache.evictPair(followerId, targetUserId);
    }

    @Override
    public UserFollowStatsResponse getFollowStats(Long userId) {
        requireUserById(userId);
        Optional<UserFollowStatsResponse> cached = followStatsRedisCache.get(userId);
        if (cached.isPresent()) {
            return cached.get();
        }
        userFollowStatsMapper.ensureRow(userId);
        UserFollowStats row = userFollowStatsMapper.selectByUserId(userId);
        long following = row != null && row.getFollowingCount() != null ? row.getFollowingCount() : 0L;
        long followers = row != null && row.getFollowerCount() != null ? row.getFollowerCount() : 0L;
        UserFollowStatsResponse stats = new UserFollowStatsResponse(following, followers);
        followStatsRedisCache.put(userId, stats);
        return stats;
    }

    @Override
    public UserFollowRelationResponse getFollowRelation(Long targetUserId) {
        Long viewerId = currentUserId();
        if (Objects.equals(viewerId, targetUserId)) {
            return new UserFollowRelationResponse(false, false);
        }
        requireUserById(targetUserId);
        boolean following = userFollowMapper.existsEdge(viewerId, targetUserId);
        boolean followsYou = userFollowMapper.existsEdge(targetUserId, viewerId);
        return new UserFollowRelationResponse(following, followsYou);
    }

    @Override
    public FollowCursorPageResponse<FollowUserItemResponse> listFollowers(Long userId, String cursor, int limit) {
        requireUserById(userId);
        CursorParams p = cursorParams(cursor);
        List<FollowListRow> rows = userFollowMapper.selectFollowersPage(
                userId, p.createdAt(), p.tieBreakerUserId(), limit + 1);
        return buildFollowPage(rows, limit);
    }

    @Override
    public FollowCursorPageResponse<FollowUserItemResponse> listFollowing(Long userId, String cursor, int limit) {
        requireUserById(userId);
        CursorParams p = cursorParams(cursor);
        List<FollowListRow> rows = userFollowMapper.selectFollowingPage(
                userId, p.createdAt(), p.tieBreakerUserId(), limit + 1);
        return buildFollowPage(rows, limit);
    }

    private CursorParams cursorParams(String cursor) {
        Optional<FollowCursorCodec.DecodedFollowCursor> decoded = followCursorCodec.decode(cursor);
        if (decoded.isEmpty()) {
            return new CursorParams(null, null);
        }
        FollowCursorCodec.DecodedFollowCursor d = decoded.get();
        return new CursorParams(d.createdAt(), d.tieBreakerUserId());
    }

    private FollowCursorPageResponse<FollowUserItemResponse> buildFollowPage(List<FollowListRow> rows, int limit) {
        boolean hasMore = rows.size() > limit;
        List<FollowListRow> window = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = null;
        if (hasMore && !window.isEmpty()) {
            FollowListRow last = window.get(window.size() - 1);
            nextCursor = followCursorCodec.encode(last.getFollowedAt(), last.getUserId());
        }
        List<FollowUserItemResponse> items = window.stream().map(FollowUserItemResponse::from).toList();
        return new FollowCursorPageResponse<>(items, nextCursor, hasMore);
    }

    private void requireFollowableUser(Long userId) {
        User user = requireUserById(userId);
        if (!user.isEnabled()) {
            throw new BusinessException(404, "用户不存在");
        }
    }

    private static boolean hasAnyProfileField(UpdateProfileRequest request) {
        return StringUtils.hasText(request.nickname())
                || request.avatar() != null
                || request.gender() != null
                || request.email() != null
                || request.phone() != null;
    }

    private Long currentUserId() {
        return UserContext.get()
                .map(LoginUser::id)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
    }

    private User requireUserByCurrentId() {
        Long userId = currentUserId();
        return requireUserById(userId);
    }

    private User requireUserById(Long userId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getId, userId)
                .isNull(User::getDeletedAt)
                .last("LIMIT 1");
        return Optional.ofNullable(userMapper.selectOne(wrapper))
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private record CursorParams(LocalDateTime createdAt, Long tieBreakerUserId) {
    }
}
