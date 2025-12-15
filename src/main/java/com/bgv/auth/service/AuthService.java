package com.bgv.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgv.auth.config.JwtProvider;
import com.bgv.auth.dto.AccountStatus;
import com.bgv.auth.dto.AuthProvider;
import com.bgv.auth.dto.AuthResponse;
import com.bgv.auth.dto.ForgotPasswordRequest;
import com.bgv.auth.dto.GoogleAuthRequest;
import com.bgv.auth.dto.LoginRequest;
import com.bgv.auth.dto.LogoutRequest;
import com.bgv.auth.dto.PasswordResetToken;
import com.bgv.auth.dto.RefreshToken;
import com.bgv.auth.dto.RefreshTokenRequest;
import com.bgv.auth.dto.RegisterRequest;
import com.bgv.auth.dto.ResetPasswordRequest;
import com.bgv.auth.dto.User;
import com.bgv.auth.dto.UserRole;
import com.bgv.auth.repository.PasswordResetTokenRepository;
import com.bgv.auth.repository.RefreshTokenRepository;
import com.bgv.auth.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import jakarta.security.auth.message.AuthException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GoogleAuthService googleAuthService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) throws AuthException {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setProviderId(null);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) throws AuthException {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        // Security rule: Google users cannot login with password
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new AuthException("This account uses Google authentication. Please use Google login.");
        }

        // Security rule: BLOCKED users must not receive tokens
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is blocked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) throws AuthException {
        // Verify Google ID token
        Payload payload = googleAuthService.verifyIdToken(request.idToken());
        
        String email = payload.getEmail();
        String providerId = payload.getSubject(); // Google "sub"

        // Check if user exists by providerId (Google user)
        User user = userRepository.findByProviderId(providerId).orElse(null);

        if (user != null) {
            // Existing Google user - login
            // Security rule: BLOCKED users must not receive tokens
            if (user.getStatus() != AccountStatus.ACTIVE) {
                throw new AuthException("Account is blocked");
            }
            return issueTokens(user);
        }

        // Check if email exists with different provider (security rule: one email = one account)
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("An account with this email already exists. Please use the original sign-in method.");
        }

        // Create new Google user
        user = new User();
        user.setEmail(email);
        user.setPasswordHash(null); // Google users don't have passwords
        user.setRole(UserRole.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        user.setCreatedAt(Instant.now());

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) throws AuthException {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (refreshToken.getExpiry().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthException("Refresh token expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        // Security rule: BLOCKED users must not receive tokens
        if (user.getStatus() != AccountStatus.ACTIVE) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthException("Account is blocked");
        }

        // Delete old refresh token and issue new ones
        refreshTokenRepository.delete(refreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.email()).orElse(null);
        
        // Security: Don't reveal if email exists or not (prevent email enumeration)
        if (user == null) {
            // Return silently to prevent email enumeration attacks
            return;
        }

        // Security: Only LOCAL users can reset password
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            // Return silently for Google users
            return;
        }

        // Invalidate any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Generate new reset token
        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(
                resetToken,
                user.getId(),
                Instant.now().plus(1, ChronoUnit.HOURS), // Token valid for 1 hour
                Instant.now()
        );

        passwordResetTokenRepository.save(passwordResetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) throws AuthException {
        // Find reset token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new AuthException("Invalid or expired reset token"));

        // Check if token is already used
        if (resetToken.isUsed()) {
            throw new AuthException("Reset token has already been used");
        }

        // Check if token is expired
        if (resetToken.getExpiry().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new AuthException("Reset token has expired");
        }

        // Find user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        // Security: Only LOCAL users can reset password
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new AuthException("Password reset is not available for Google accounts");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Invalidate all refresh tokens for security
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        // Delete the refresh token
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(refreshTokenRepository::delete);
        // Note: We don't throw exception if token doesn't exist to prevent information leakage
    }

    @Transactional
    public void logoutAll(UUID userId) {
        // Delete all refresh tokens for the user (logout from all devices)
        refreshTokenRepository.deleteByUserId(userId);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateToken(user.getId(), user.getRole(), user.getEmail());
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
                user.getId(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now()
        );

        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, refreshTokenValue);
    }
}
