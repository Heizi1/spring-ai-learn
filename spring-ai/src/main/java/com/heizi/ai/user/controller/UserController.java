package com.heizi.ai.user.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heizi.ai.auth.model.AuthenticatedUser;
import com.heizi.ai.user.dto.UpdateUserProfileRequest;
import com.heizi.ai.user.dto.UserProfileResponse;
import com.heizi.ai.user.model.User;
import com.heizi.ai.user.service.UserService;
import com.heizi.common.result.ApiResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(UserProfileResponse.from(userService.findRequired(currentUser.userId())));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @Valid @RequestBody UpdateUserProfileRequest request) {
        User user = userService.updateProfile(currentUser.userId(), request);
        return ApiResponse.success(UserProfileResponse.from(user));
    }

}
