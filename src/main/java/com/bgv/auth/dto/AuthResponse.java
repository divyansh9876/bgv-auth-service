package com.bgv.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
