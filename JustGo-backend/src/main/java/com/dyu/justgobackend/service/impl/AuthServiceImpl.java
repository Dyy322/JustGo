package com.dyu.justgobackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dyu.justgobackend.dto.request.LoginRequest;
import com.dyu.justgobackend.dto.response.LoginResponse;
import com.dyu.justgobackend.dto.request.RefreshTokenRequest;
import com.dyu.justgobackend.dto.request.RegisterRequest;
import com.dyu.justgobackend.dto.response.UserProfileResponse;
import com.dyu.justgobackend.entity.User;
import com.dyu.justgobackend.exception.BusinessException;
import com.dyu.justgobackend.mapper.UserMapper;
import com.dyu.justgobackend.security.JwtDenylistService;
import com.dyu.justgobackend.security.JwtTokenProvider;
import com.dyu.justgobackend.security.LoginUser;
import com.dyu.justgobackend.security.ParsedToken;
import com.dyu.justgobackend.security.UserContext;
import com.dyu.justgobackend.service.AuthService;
import com.dyu.justgobackend.service.UserService;
import com.dyu.justgobackend.util.AuthorizationHeaderUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtDenylistService jwtDenylistService;
    private final UserService userService;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                           JwtDenylistService jwtDenylistService, UserService userService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtDenylistService = jwtDenylistService;
        this.userService = userService;
    }

    @Override
    public LoginResponse login(LoginRequest request, String clientIp) {
        User user = findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!user.isEnabled()) {
            throw new BusinessException(403, "账号当前不可用");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            recordLoginFailure(user.getId());
            throw new BusinessException(401, "用户名或密码错误");
        }

        recordLoginSuccess(user.getId(), clientIp);
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtTokenProvider.expiresInSeconds(),
                refreshToken,
                jwtTokenProvider.refreshExpiresInSeconds(),
                UserProfileResponse.from(user));
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        ParsedToken parsed = jwtTokenProvider.parseRefreshToken(request.refreshToken());
        if (jwtDenylistService.isDenied(parsed.jti())) {
            throw new BusinessException(401, "刷新令牌已失效，请重新登录");
        }

        User user = findById(parsed.loginUser().id())
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (!user.isEnabled()) {
            throw new BusinessException(403, "账号当前不可用");
        }

        long refreshRemainingSeconds =
                parsed.expiresAtEpochSecond() - Instant.now().getEpochSecond();
        jwtDenylistService.denyUntilExpiry(parsed.jti(), Duration.ofSeconds(Math.max(0, refreshRemainingSeconds)));

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtTokenProvider.expiresInSeconds(),
                refreshToken,
                jwtTokenProvider.refreshExpiresInSeconds(),
                UserProfileResponse.from(user));
    }

    @Override
    public UserProfileResponse register(RegisterRequest request) {
        if (findByUsername(request.username()).isPresent()) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setNickname(request.nickname());
        user.setStatus(1);
        user.setAccountType(1);
        userMapper.insert(user);
        return UserProfileResponse.from(user);
    }

    @Override
    public UserProfileResponse currentUserProfile() {
        return userService.getCurrentUserProfile();
    }

    /** 将当前令牌的 jti 写入 Redis 黑名单，TTL 为剩余有效期，使登出立即生效并防止重放。 */
    @Override
    public void logout(String authorizationHeader) {
        String rawToken = AuthorizationHeaderUtils.bearerToken(authorizationHeader)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
        ParsedToken parsed = jwtTokenProvider.parseAccessToken(rawToken);

        LoginUser loginUser =
                UserContext.get().orElseThrow(() -> new BusinessException(401, "请先登录"));
        if (!loginUser.id().equals(parsed.loginUser().id())) {
            throw new BusinessException(403, "登录状态异常");
        }

        long remainingSeconds =
                parsed.expiresAtEpochSecond() - Instant.now().getEpochSecond();
        jwtDenylistService.denyUntilExpiry(parsed.jti(), Duration.ofSeconds(Math.max(0, remainingSeconds)));
    }

    private Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .isNull(User::getDeletedAt)
                .last("limit 1");
        return Optional.ofNullable(userMapper.selectOne(wrapper));
    }

    private Optional<User> findById(Long id) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getId, id)
                .isNull(User::getDeletedAt)
                .last("limit 1");
        return Optional.ofNullable(userMapper.selectOne(wrapper));
    }

    private void recordLoginSuccess(Long userId, String clientIp) {
        User updateUser = new User();
        updateUser.setLastLoginTime(LocalDateTime.now());
        updateUser.setLastLoginIp(clientIp);
        updateUser.setFailedLoginAttempts(0);
        updateUser.setLastActiveTime(LocalDateTime.now());

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("login_count = coalesce(login_count, 0) + 1");
        userMapper.update(updateUser, wrapper);
    }

    private void recordLoginFailure(Long userId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("failed_login_attempts = coalesce(failed_login_attempts, 0) + 1");
        userMapper.update(null, wrapper);
    }
}
