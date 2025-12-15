package com.bgv.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bgv.auth.dto.ApiResponse;
import com.bgv.auth.dto.AuthResponse;
import com.bgv.auth.dto.ForgotPasswordRequest;
import com.bgv.auth.dto.GoogleAuthRequest;
import com.bgv.auth.dto.LoginRequest;
import com.bgv.auth.dto.LogoutRequest;
import com.bgv.auth.dto.RefreshTokenRequest;
import com.bgv.auth.dto.RegisterRequest;
import com.bgv.auth.dto.ResetPasswordRequest;
import com.bgv.auth.service.AuthService;

import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) throws AuthException {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) throws AuthException {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request) throws AuthException {
        return ApiResponse.success(authService.authenticateWithGoogle(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) throws AuthException {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Always return success to prevent email enumeration
        return ApiResponse.success("If the email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) throws AuthException {
        authService.resetPassword(request);
        return ApiResponse.success("Password has been reset successfully");
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success("Logged out successfully");
    }

    @PostMapping("/logout-all")
    public ApiResponse<String> logoutAll() throws AuthException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID) {
            UUID userId = (UUID) authentication.getPrincipal();
            authService.logoutAll(userId);
            return ApiResponse.success("Logged out from all devices successfully");
        }
        throw new AuthException("User not authenticated");
    }
}
