package com.heizi.ai.auth.model;

import java.time.Instant;

public record JwtClaims(
        String jwtId,
        Long userId,
        String username,
        String nickname,
        Instant issuedAt,
        Instant expiresAt
) {
}
