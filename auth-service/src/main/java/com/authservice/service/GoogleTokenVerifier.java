package com.authservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Service for verifying Google OAuth2 ID tokens
 */
@Service
@Slf4j
public class GoogleTokenVerifier {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Verifies the Google ID token and extracts user information
     * 
     * @param idTokenString The ID token from Google Sign-In
     * @return GoogleIdToken.Payload containing user information
     * @throws GeneralSecurityException if token verification fails
     * @throws IOException if there's a network error
     */
    public GoogleIdToken.Payload verifyToken(String idTokenString) 
            throws GeneralSecurityException, IOException {
        
        log.info("Verifying Google ID token");

        //private final GoogleIdTokenVerifier verifier;
        GoogleIdToken idToken = verifier.verify(idTokenString);
        
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            
            // Print user identifier
            String userId = payload.getSubject();
            log.info("User ID: {}", userId);

            // Get profile information from payload
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            String locale = (String) payload.get("locale");
            String familyName = (String) payload.get("family_name");
            String givenName = (String) payload.get("given_name");
            
            log.info("Email: {}, Verified: {}, Name: {}", email, emailVerified, name);
            
            return payload;
        } else {
            log.error("Invalid ID token");
            throw new GeneralSecurityException("Invalid ID token");
        }
    }

    /**
     * Extract email from verified token payload
     */
    public String getEmail(GoogleIdToken.Payload payload) {
        return payload.getEmail();
    }

    /**
     * Extract full name from verified token payload
     */
    public String getFullName(GoogleIdToken.Payload payload) {
        return (String) payload.get("name");
    }

    /**
     * Extract Google user ID from verified token payload
     */
    public String getGoogleId(GoogleIdToken.Payload payload) {
        return payload.getSubject();
    }

    /**
     * Check if email is verified
     */
    public boolean isEmailVerified(GoogleIdToken.Payload payload) {
        return Boolean.TRUE.equals(payload.getEmailVerified());
    }

    /**
     * Extract profile picture URL
     */
    public String getPictureUrl(GoogleIdToken.Payload payload) {
        return (String) payload.get("picture");
    }

    /**
     * Extract given name (first name)
     */
    public String getGivenName(GoogleIdToken.Payload payload) {
        return (String) payload.get("given_name");
    }

    /**
     * Extract family name (last name)
     */
    public String getFamilyName(GoogleIdToken.Payload payload) {
        return (String) payload.get("family_name");
    }

    /**
     * Extract user locale/language preference
     */
    public String getLocale(GoogleIdToken.Payload payload) {
        return (String) payload.get("locale");
    }
}