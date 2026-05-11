package com.heizi.ai.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.heizi.ai.auth.config.JwtProperties;
import com.heizi.ai.auth.model.RefreshTokenRecord;
import com.heizi.common.exception.BusinessException;

@Service
public class RefreshTokenService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String REFRESH_INDEX_PREFIX = "auth:refresh:index:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    public String create(Long userId, String username) {
        String tokenId = UUID.randomUUID().toString();
        String secret = randomSecret();
        String token = tokenId + "." + secret;

        String tokenKey = refreshTokenKey(userId, tokenId);
        String indexKey = refreshIndexKey(tokenId);
        String value = sha256(secret) + "|" + username + "|" + Instant.now();

        redisTemplate.opsForValue().set(tokenKey, value, jwtProperties.refreshTokenTtl());
        redisTemplate.opsForValue().set(indexKey, String.valueOf(userId), jwtProperties.refreshTokenTtl());
        return token;
    }

    public RefreshTokenRecord validate(String refreshToken) {
        TokenParts tokenParts = parse(refreshToken);
        String indexValue = redisTemplate.opsForValue().get(refreshIndexKey(tokenParts.tokenId()));
        if (indexValue == null) {
            throw new BusinessException("refresh token 已失效");
        }

        Long userId = Long.valueOf(indexValue);
        String tokenValue = redisTemplate.opsForValue().get(refreshTokenKey(userId, tokenParts.tokenId()));
        if (tokenValue == null) {
            throw new BusinessException("refresh token 已失效");
        }

        String expectedHash = tokenValue.split("\\|", 2)[0];
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8),
                sha256(tokenParts.secret()).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("refresh token 无效");
        }
        return new RefreshTokenRecord(userId, tokenParts.tokenId());
    }

    public void revoke(String refreshToken) {
        RefreshTokenRecord record = validate(refreshToken);
        redisTemplate.delete(refreshTokenKey(record.userId(), record.tokenId()));
        redisTemplate.delete(refreshIndexKey(record.tokenId()));
    }

    public void revokeAll(Long userId) {
        Set<String> keys = redisTemplate.keys(refreshTokenKey(userId, "*"));
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String tokenId = key.substring(key.lastIndexOf(':') + 1);
            redisTemplate.delete(refreshIndexKey(tokenId));
        }
        redisTemplate.delete(keys);
    }

    private TokenParts parse(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("refreshToken 不能为空");
        }
        String[] parts = refreshToken.split("\\.");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException("refresh token 无效");
        }
        return new TokenParts(parts[0], parts[1]);
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return BASE64_URL_ENCODER.encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return BASE64_URL_ENCODER.encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("refresh token hash 失败", exception);
        }
    }

    private String refreshTokenKey(Long userId, String tokenId) {
        return REFRESH_PREFIX + userId + ":" + tokenId;
    }

    private String refreshIndexKey(String tokenId) {
        return REFRESH_INDEX_PREFIX + tokenId;
    }

    private record TokenParts(String tokenId, String secret) {
    }

}
