package com.dyu.justgobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dyu.justgobackend.dto.request.ChangePasswordRequest;
import com.dyu.justgobackend.dto.request.UpdateProfileRequest;
import com.dyu.justgobackend.dto.response.UserProfileResponse;
import com.dyu.justgobackend.dto.response.UserPublicProfileResponse;
import com.dyu.justgobackend.entity.User;
import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.mapper.UserMapper;
import com.dyu.justgobackend.security.LoginUser;
import com.dyu.justgobackend.security.UserContext;
import com.dyu.justgobackend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
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
}
