package com.heizi.ai.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 50, message = "nickname 长度不能超过 50")
        String nickname,

        @Size(max = 300, message = "avatarUrl 长度不能超过 300")
        String avatarUrl
) {
}
