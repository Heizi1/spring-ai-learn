package com.heizi.ai.auth.controller;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heizi.ai.auth.config.JwtProperties;
import com.heizi.ai.auth.dto.AuthResponse;
import com.heizi.ai.auth.dto.LoginRequest;
import com.heizi.ai.auth.dto.RefreshTokenRequest;
import com.heizi.ai.auth.dto.RegisterRequest;
import com.heizi.ai.auth.model.AuthenticatedUser;
import com.heizi.ai.auth.service.AuthService;
import com.heizi.common.result.ApiResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        writeRefreshCookie(response, authResponse.refreshToken());
        return ApiResponse.success(authResponse);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        writeRefreshCookie(response, authResponse.refreshToken());
        return ApiResponse.success(authResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                             HttpServletRequest servletRequest,
                                             HttpServletResponse response) {
        AuthResponse authResponse = authService.refresh(resolveRefreshToken(request, servletRequest));
        writeRefreshCookie(response, authResponse.refreshToken());
        return ApiResponse.success(authResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                    HttpServletRequest servletRequest,
                                    HttpServletResponse response) {
        authService.logout(resolveRefreshTokenOrNull(request, servletRequest));
        clearRefreshCookie(response);
        return ApiResponse.success();
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       HttpServletResponse response) {
        authService.logoutAll(currentUser.userId());
        clearRefreshCookie(response);
        return ApiResponse.success();
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        String refreshToken = resolveRefreshTokenOrNull(request, servletRequest);
        if (StrUtil.isBlank(refreshToken)) {
            throw new com.heizi.common.exception.BusinessException("refreshToken 不能为空");
        }
        return refreshToken;
    }

    private String resolveRefreshTokenOrNull(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        if (request != null && StrUtil.isNotBlank(request.refreshToken())) {
            return request.refreshToken();
        }
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (jwtProperties.refreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtProperties.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
