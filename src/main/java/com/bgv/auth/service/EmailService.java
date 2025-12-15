package com.bgv.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetToken);
}


