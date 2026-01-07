package com.bgv.auth.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.security.auth.message.AuthException;

@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verifyIdToken(String idToken) throws AuthException {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new AuthException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            
            // Verify email is verified
            Boolean emailVerified = (Boolean) payload.get("email_verified");
            if (emailVerified == null || !emailVerified) {
                throw new AuthException("Google email is not verified");
            }

            return payload;
        } catch (AuthException e) {
            throw e;
        } catch (GeneralSecurityException | IOException e) {
            throw new AuthException("Failed to verify Google ID token: " + e.getMessage());
        }
    }
}

