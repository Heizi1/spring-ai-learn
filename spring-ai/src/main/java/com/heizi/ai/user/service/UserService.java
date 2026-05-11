package com.heizi.ai.user.service;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import com.heizi.ai.user.dto.UpdateUserProfileRequest;
import com.heizi.ai.user.model.User;
import com.heizi.ai.user.model.UserStatus;
import com.heizi.ai.user.repository.UserRepository;
import com.heizi.common.exception.BusinessException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String passwordHash, String nickname) {
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("username 已存在");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordHash);
        user.setNickname(StrUtil.blankToDefault(nickname, normalizedUsername));
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    public User findRequired(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public User findRequiredByUsername(String username) {
        return userRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new BusinessException("username 或 password 错误"));
    }

    public User updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = findRequired(userId);
        if (request.nickname() != null) {
            user.setNickname(StrUtil.blankToDefault(request.nickname(), user.getUsername()));
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(StrUtil.isBlank(request.avatarUrl()) ? null : request.avatarUrl());
        }
        return userRepository.save(user);
    }

    public void touchLastLoginAt(User user) {
        user.setLastLoginAt(java.time.Instant.now());
        userRepository.save(user);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

}
