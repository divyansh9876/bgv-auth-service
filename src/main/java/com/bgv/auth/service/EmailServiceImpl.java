package com.bgv.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        // TODO: Integrate with actual email service (SendGrid, AWS SES, etc.)
        // For now, log the reset link for development
        
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        log.info("=== PASSWORD RESET EMAIL ===");
        log.info("To: {}", to);
        log.info("Reset Link: {}", resetLink);
        log.info("Token: {}", resetToken);
        log.info("============================");
        
        // In production, use an email service like:
        // - Spring Mail (JavaMailSender)
        // - SendGrid
        // - AWS SES
        // - Mailgun
    }
}


