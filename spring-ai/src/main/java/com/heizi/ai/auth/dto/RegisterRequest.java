package com.heizi.ai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username 不能为空")
        @Size(min = 3, max = 50, message = "username 长度必须在 3 到 50 之间")
        String username,

        @NotBlank(message = "password 不能为空")
        @Size(min = 6, max = 100, message = "password 长度必须在 6 到 100 之间")
        String password,

        @Size(max = 50, message = "nickname 长度不能超过 50")
        String nickname
) {
}
