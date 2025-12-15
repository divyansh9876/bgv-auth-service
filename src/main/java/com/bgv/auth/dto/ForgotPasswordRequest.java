package com.bgv.auth.dto;

import jakarta.validation.constraints.Email;

public record ForgotPasswordRequest(
        @Email(message = "Invalid email format") String email
) {}


