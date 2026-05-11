package com.heizi.ai.auth.dto;

import com.heizi.ai.user.dto.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserProfileResponse user
) {
}
