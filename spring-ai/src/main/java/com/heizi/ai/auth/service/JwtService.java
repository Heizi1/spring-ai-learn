package com.heizi.ai.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heizi.ai.auth.config.JwtProperties;
import com.heizi.ai.auth.model.JwtClaims;
import com.heizi.ai.user.model.User;
import com.heizi.common.exception.BusinessException;

@Service
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final byte[] secretBytes;

    public JwtService(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.secretBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret 长度至少需要 32 字节");
        }
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", jwtProperties.issuer());
        payload.put("sub", user.getUsername());
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        payload.put("userId", user.getId());
        payload.put("nickname", user.getNickname());

        String signingInput = encodeJson(header) + "." + encodeJson(payload);
        return signingInput + "." + sign(signingInput);
    }

    public JwtClaims parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException("无效 access token");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException("无效 access token");
            }

            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]),
                    new TypeReference<>() {
                    });
            if (!jwtProperties.issuer().equals(payload.get("iss"))) {
                throw new BusinessException("无效 access token");
            }

            Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
            if (!expiresAt.isAfter(Instant.now())) {
                throw new BusinessException("access token 已过期");
            }

            return new JwtClaims(
                    String.valueOf(payload.get("jti")),
                    asLong(payload.get("userId")),
                    String.valueOf(payload.get("sub")),
                    String.valueOf(payload.get("nickname")),
                    Instant.ofEpochSecond(asLong(payload.get("iat"))),
                    expiresAt
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("无效 access token");
        }
    }

    public long accessTokenTtlSeconds() {
        return jwtProperties.accessTokenTtl().toSeconds();
    }

    private String encodeJson(Map<String, Object> content) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(content));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 序列化失败", exception);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 签名失败", exception);
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

}
