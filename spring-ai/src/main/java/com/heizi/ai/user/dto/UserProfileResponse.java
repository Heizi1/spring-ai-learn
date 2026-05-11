package com.heizi.ai.user.dto;

import java.time.Instant;

import com.heizi.ai.user.model.User;
import com.heizi.ai.user.model.UserStatus;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

}
