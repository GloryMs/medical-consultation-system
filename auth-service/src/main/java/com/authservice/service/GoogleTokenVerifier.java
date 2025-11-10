package com.authservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.annotation.PostConstruct;
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

    private GoogleIdTokenVerifier verifier;  // No longer final, since it's initialized post-construction


    public GoogleTokenVerifier() {}

    @PostConstruct
    public void init() {
        log.info("Initializing GoogleTokenVerifier with clientId={}", clientId);
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                // Add clock skew allowance to account for time differences
                .setAcceptableTimeSkewSeconds(300) // 5 minutes allowance
                .build();
    }


//    public GoogleTokenVerifier() {
//        log.info("Initializing GoogleTokenVerifier with clientId: {}", clientId);
//
//        this.verifier = new GoogleIdTokenVerifier.Builder(
//                new NetHttpTransport(),
//                new GsonFactory()
//        )
//                .setAudience(Collections.singletonList(clientId))
//                .build();
//    }



    public GoogleIdTokenVerifier googleIdTokenVerifier() throws Exception {

        log.info("🔧 Initializing GoogleIdTokenVerifier");
        log.info("   Client ID: {}", clientId);

        // ✅ This MUST be the same as token's "aud" claim
        verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))  // Uses from properties
                .build();

        log.info("✅ GoogleIdTokenVerifier ready with Client ID");
        return verifier;
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

        GoogleIdToken idToken = null;

        try{
            idToken = verifier.verify(idTokenString);
        }catch(Exception e){
            e.printStackTrace();
        }

        if (idToken == null) {
            log.error("Token verification returned null");
            // Parse without verification to inspect claims for debugging
            try {
                idTokenString = idTokenString.replace("Bearer ", "").trim();
                GoogleIdToken parsedToken = GoogleIdToken.parse(new GsonFactory(), idTokenString);
                if (parsedToken != null) {
                    GoogleIdToken.Payload parsedPayload = parsedToken.getPayload();
                    log.info("Parsed audience (aud): {}", parsedPayload.getAudience());
                    log.info("Parsed issuer (iss): {}", parsedPayload.getIssuer());
                    log.info("Parsed subject (sub): {}", parsedPayload.getSubject());

                    // Check if token is expired based on current time
                    long currentTime = System.currentTimeMillis() / 1000;
                    long expirationTime = parsedPayload.getExpirationTimeSeconds();
                    long issuedAtTime = parsedPayload.getIssuedAtTimeSeconds();

                    log.info("Current server time: {} (seconds since epoch)", currentTime);
                    log.info("Token expiration time: {} (seconds since epoch)", expirationTime);
                    log.info("Token issued at: {} (seconds since epoch)", issuedAtTime);
                    log.info("Time until expiration: {} seconds", expirationTime - currentTime);
                    log.info("Time since issuance: {} seconds", currentTime - issuedAtTime);
                    // Add more if needed, e.g., email
                } else {
                    log.error("Even parsing without verification returned null - malformed token?");
                }
            } catch (Exception e) {
                log.error("Failed to parse token for debugging", e);
            }
            throw new GeneralSecurityException("Invalid ID token - verification failed.");
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