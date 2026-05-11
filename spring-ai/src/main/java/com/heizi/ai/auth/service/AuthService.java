package com.heizi.ai.auth.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.heizi.ai.auth.dto.AuthResponse;
import com.heizi.ai.auth.dto.LoginRequest;
import com.heizi.ai.auth.dto.RegisterRequest;
import com.heizi.ai.auth.model.RefreshTokenRecord;
import com.heizi.ai.user.dto.UserProfileResponse;
import com.heizi.ai.user.model.User;
import com.heizi.ai.user.model.UserStatus;
import com.heizi.ai.user.service.UserService;
import com.heizi.common.exception.BusinessException;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserService userService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.nickname());
        log.info("用户注册成功，userId={}, username={}", user.getId(), user.getUsername());
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findRequiredByUsername(request.username());
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("username 或 password 错误");
        }

        userService.touchLastLoginAt(user);
        log.info("用户登录成功，userId={}, username={}", user.getId(), user.getUsername());
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        RefreshTokenRecord record = refreshTokenService.validate(refreshToken);
        refreshTokenService.revoke(refreshToken);
        User user = userService.findRequired(record.userId());
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return;
        }
        refreshTokenService.revoke(refreshToken);
    }

    public void logoutAll(Long userId) {
        refreshTokenService.revokeAll(userId);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user.getId(), user.getUsername());
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.accessTokenTtlSeconds(),
                UserProfileResponse.from(user));
    }

}
