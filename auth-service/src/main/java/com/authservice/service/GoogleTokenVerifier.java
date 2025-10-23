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

    public GoogleTokenVerifier() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

//    @Bean
//    public GoogleIdTokenVerifier googleIdTokenVerifier() throws Exception {
//
//        log.info("🔧 Initializing GoogleIdTokenVerifier");
//        log.info("   Client ID: {}", clientId);
//
//        // ✅ This MUST be the same as token's "aud" claim
//        verifier = new GoogleIdTokenVerifier.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(),
//                JacksonFactory.getDefaultInstance())
//                .setAudience(Collections.singletonList(clientId))  // Uses from properties
//                .build();
//
//        log.info("✅ GoogleIdTokenVerifier ready with Client ID");
//        return verifier;
//    }

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

        //Verify id token
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            log.error("ID token string is empty or null");
            throw new GeneralSecurityException("ID token cannot be empty");
        }

        //Validate verifier
        if (verifier == null) {
            log.error("GoogleIdTokenVerifier is NULL - Configuration failed");
            throw new GeneralSecurityException(
                    "Google OAuth configuration error: Verifier not initialized");
        }
        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            log.error("Token verification returned null");
            throw new GeneralSecurityException(
                    "Invalid ID token - verification failed. ");
        }
        else{
            log.info("✅ Google ID token verification SUCCESSFUL");
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