package com.authservice.service;

import com.authservice.dto.*;
import com.authservice.kafka.AuthEventProducer;
import com.authservice.repository.PasswordResetCodeRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtService;
import com.authservice.entity.User;
import com.authservice.util.JwtUtil;
import com.commonlibrary.dto.EmailNotificationDto;
import com.commonlibrary.dto.SmsNotificationDto;
import com.commonlibrary.dto.UserDto;
import com.commonlibrary.dto.UserStasDto;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.entity.UserStatus;
import com.commonlibrary.exception.BusinessException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.authservice.entity.PasswordResetCode;
import org.springframework.ui.ModelMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuthEventProducer authEventProducer;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private final GoogleTokenVerifier googleTokenVerifier;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT);
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone Number already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.PENDING_VERIFICATION)
                .fullName(request.getFullName())
                .emailVerified(false)
                .isDeleted(false)
                .timeZone("UTC")
                .preferredLanguage("EN")
                .build();

        User savedUser = userRepository.save(user);

// 🔥 NEW: Send Kafka event after successful registration
        authEventProducer.sendUserRegistrationEvent(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getFullName()
        );

        System.out.println("New user registration .. kafka event sent: " + savedUser.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails, savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(savedUser.getRole().name())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException("Account is " + user.getStatus(), HttpStatus.FORBIDDEN);
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

//    @Transactional
//    public AuthResponse googleLogin(GoogleLoginRequest request) {
//        // Simulate Google OAuth - In production, verify the ID token with Google
//        String googleId = "google_" + System.currentTimeMillis();
//        String email = "user_" + googleId + "@gmail.com";
//
//        User user = userRepository.findByGoogleOAuthId(googleId)
//                .orElseGet(() -> {
//                    User newUser = User.builder()
//                            .email(email)
//                            .passwordHash(passwordEncoder.encode("GoogleAuth" + googleId))
//                            .googleOAuthId(googleId)
//                            .role(request.getRole())
//                            .status(UserStatus.ACTIVE)
//                            .emailVerified(true)
//                            .build();
//                    return userRepository.save(newUser);
//                });
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
//        String token = jwtService.generateToken(userDetails, user.getRole().name());
//
//        return AuthResponse.builder()
//                .token(token)
//                .role(user.getRole().name())
//                .userId(user.getId())
//                .email(user.getEmail())
//                .build();
//    }

    /**
     * Google OAuth 2.0 Login Implementation
     * Verifies Google ID token and creates/updates user account
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        User user = null;
        try {
            log.info("googleLogin: Processing Google login request");
            log.info("googleLogin   ===> IdToken: {}", request.getIdToken());

            // Verify the Google ID token
            GoogleIdToken.Payload payload = googleTokenVerifier.verifyToken(request.getIdToken());

            // Extract user information from the verified token
            String googleId = googleTokenVerifier.getGoogleId(payload);
            String email = googleTokenVerifier.getEmail(payload);
            String fullName = googleTokenVerifier.getFullName(payload);
            String pictureUrl = googleTokenVerifier.getPictureUrl(payload);
            boolean emailVerified = googleTokenVerifier.isEmailVerified(payload);
            String givenName = googleTokenVerifier.getGivenName(payload);
            String familyName = googleTokenVerifier.getFamilyName(payload);
            String locale = googleTokenVerifier.getLocale(payload);

            log.info("Google token verified for email: {}", email);

            // Check if user already exists by Google ID or email
            Optional<User> existingUser = userRepository.findByGoogleOAuthId(googleId);

            if (existingUser.isEmpty()) {
                existingUser = userRepository.findByEmail(email);
            }


            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                // Update existing user
                user = existingUser.get();

                // If user exists but doesn't have Google OAuth ID, link it
                if (user.getGoogleOAuthId() == null) {
                    user.setGoogleOAuthId(googleId);
                }

                // Update user information from Google
                user.setFullName(fullName != null ? fullName : user.getFullName());
                user.setEmailVerified(emailVerified);
                user.setLastLogin(LocalDateTime.now());

                // Set preferred language if available and not set
                if (locale != null && (user.getPreferredLanguage() == null || user.getPreferredLanguage().equals("EN"))) {
                    user.setPreferredLanguage(locale.toUpperCase());
                }

                //Check the role if the same:
                if( !request.getRole().equals(user.getRole()) ){
                    throw new BusinessException("Role does not match, you've already registered in with different role",
                            HttpStatus.CONFLICT);
                }

                log.info("Updated existing user: {}", email);
            } else {
                // Create new user
                isNewUser = true;
                user = User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("GOOGLE_OAUTH_" + googleId)) // Random password for OAuth users
                        .googleOAuthId(googleId)
                        .role(request.getRole()) // Role must be provided in the request
                        .status(UserStatus.ACTIVE) // OAuth users are automatically active
                        .fullName(fullName)
                        .emailVerified(emailVerified)
                        .isDeleted(false)
                        .timeZone("UTC")
                        .preferredLanguage(locale != null ? locale.toUpperCase() : "EN")
                        .lastLogin(LocalDateTime.now())
                        .build();

                log.info("Creating new Google OAuth user: {}", email);
            }

            user = userRepository.save(user);

            // Send Kafka event for new Google OAuth users
            if (isNewUser) {
                authEventProducer.sendUserRegistrationEvent(
                        user.getId(),
                        user.getEmail(),
                        null,
                        user.getRole().name(),
                        user.getFullName()
                );

                log.info("Google OAuth registration - kafka event sent: {}", user.getEmail());
            }

            // Generate JWT token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String token = jwtService.generateToken(userDetails, user.getRole().name());

            return AuthResponse.builder()
                    .token(token)
                    .role(user.getRole().name())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build();

        } catch (GeneralSecurityException e) {
            log.error("Invalid Google ID token", e);
            e.printStackTrace();
            throw new BusinessException("Invalid Google ID token", HttpStatus.UNAUTHORIZED);
        } catch (IOException e) {
            log.error("Error verifying Google ID token", e);
            e.printStackTrace();
            throw new BusinessException("Error verifying Google credentials", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) {
            log.error("Unexpected error during Google login", e);

            e.printStackTrace();
            if( !request.getRole().equals(user.getRole()) ){
                throw new BusinessException("Role does not match, you've already registered in with different role",
                        HttpStatus.CONFLICT);
            }
            else
            throw new BusinessException("Google login failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Page<UserDto> getAllUsers(int page, int size, UserRole role, UserStatus status){
        Pageable pageable = PageRequest.of(page, size);

        if (role != null && status != null) {
            return userRepository.findByRoleAndStatus(role, status, pageable)
                    .map(this::convertToUserDto);
        } else if (role != null) {
            return userRepository.findByRole(role, pageable)
                    .map(this::convertToUserDto);
        } else if (status != null) {
            return userRepository.findByStatus(status, pageable)
                    .map(this::convertToUserDto);
        } else {
            return userRepository.findAll(pageable)
                    .map(this::convertToUserDto);
        }
    }

    public UserDto convertToUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setRole(user.getRole().name());
        userDto.setFullName(user.getFullName());
        userDto.setStatus(user.getStatus().name());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setEmailVerified(user.getEmailVerified());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());
        userDto.setLastLoginAt(user.getLastLogin());
        return userDto;
    }

    public UserStasDto getUsersStats(){
        UserStasDto userStasDto = new UserStasDto();
        List<UserRole> allRoles = new ArrayList<>();
        allRoles.add(UserRole.ADMIN);
        allRoles.add(UserRole.PATIENT);
        allRoles.add(UserRole.DOCTOR);
        userStasDto.setTotalUsers(userRepository.countByRoleIn(allRoles));
        userStasDto.setActiveUsers(userRepository.countByStatus(UserStatus.ACTIVE));
        userStasDto.setPatients(userRepository.countByStatus(UserStatus.PENDING_VERIFICATION));
        userStasDto.setDoctors(userRepository.countByRole(UserRole.DOCTOR));
        userStasDto.setPatients(userRepository.countByRole(UserRole.PATIENT));
        userStasDto.setAdmins(userRepository.countByRole(UserRole.ADMIN));
        return userStasDto;
    }

    // ============================================
    // NEW METHODS - ADD THESE BELOW
    // ============================================

    /**
     * NEW METHOD: Request password reset - Send verification code
     */
    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        String identifier = request.getIdentifier().trim();

        // Determine if identifier is email or phone
        boolean isEmail = identifier.matches(EMAIL_REGEX);

        // Find user by email or phone
        User user = isEmail
                ? userRepository.findByEmail(identifier)
                .orElseThrow(() -> new BusinessException("User not found with this email", HttpStatus.NOT_FOUND))
                : userRepository.findByPhoneNumber(identifier)
                .orElseThrow(() -> new BusinessException("User not found with this phone number", HttpStatus.NOT_FOUND));

        // Delete any existing unused codes for this identifier
        resetCodeRepository.deleteByIdentifier(identifier);

        // Generate 6-digit code
        String code = generateVerificationCode();

        // Save reset code
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .identifier(identifier)
                .code(code)
                .expiryTime(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES))
                .isUsed(false)
                .attemptCount(0)
                .deliveryMethod(isEmail ? "EMAIL" : "SMS")
                .build();

        //TODO you have to remove below line:
        log.info("Verification code: {}", code);


        resetCodeRepository.save(resetCode);

        // Send verification code
        if (isEmail) {
            sendEmailVerificationCode(user.getEmail(), user.getFullName(), code);
            return "Verification code sent to your email";
        } else {
            sendSmsVerificationCode(identifier, code);
            return "Verification code sent to your phone via SMS";
        }
    }

    /**
     * NEW METHOD: Verify reset code and reset password
     */
    @Transactional
    public String verifyCodeAndResetPassword(VerifyResetCodeRequest request) {
        String identifier = request.getIdentifier().trim();
        String code = request.getCode().trim();

        // Find valid reset code
        PasswordResetCode resetCode = resetCodeRepository
                .findByIdentifierAndCodeAndIsUsedFalseAndExpiryTimeAfter(
                        identifier,
                        code,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> {
                    // Check if code exists but is expired or used
                    resetCodeRepository.findFirstByIdentifierAndIsUsedFalseOrderByCreatedAtDesc(identifier)
                            .ifPresent(existingCode -> {
                                if (existingCode.getExpiryTime().isBefore(LocalDateTime.now())) {
                                    throw new BusinessException("Verification code has expired. Please request a new one.",
                                            HttpStatus.BAD_REQUEST);
                                }
                                if (existingCode.getIsUsed()) {
                                    throw new BusinessException("Verification code has already been used. Please request a new one.",
                                            HttpStatus.BAD_REQUEST);
                                }
                            });
                    throw new BusinessException("Invalid verification code", HttpStatus.BAD_REQUEST);
                });

        // Check attempt count
        if (resetCode.getAttemptCount() >= MAX_ATTEMPTS) {
            resetCode.setIsUsed(true);
            resetCodeRepository.save(resetCode);
            throw new BusinessException("Maximum verification attempts exceeded. Please request a new code.",
                    HttpStatus.BAD_REQUEST);
        }

        // Increment attempt count
        resetCode.setAttemptCount(resetCode.getAttemptCount() + 1);
        resetCodeRepository.save(resetCode);

        // Find user
        boolean isEmail = identifier.matches(EMAIL_REGEX);
        User user = isEmail
                ? userRepository.findByEmail(identifier)
                .orElseThrow(() -> new BusinessException("User not found",
                        HttpStatus.BAD_REQUEST))
                : userRepository.findByPhoneNumber(identifier)
                .orElseThrow(() -> new BusinessException("User not found",
                        HttpStatus.BAD_REQUEST));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark code as used
        resetCode.setIsUsed(true);
        resetCodeRepository.save(resetCode);

        // Send confirmation notification
        if (isEmail) {
            sendPasswordResetConfirmation(user.getEmail(), user.getFullName());
        }

        log.info("Password reset successful for user: {}", user.getId());
        return "Password reset successful";
    }

    /**
     * NEW METHOD: Generate 6-digit verification code
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * NEW METHOD: Send verification code via email
     */
    private void sendEmailVerificationCode(String email, String fullName, String code) {
        EmailNotificationDto notification = new EmailNotificationDto();
        notification.setRecipient(email);
        notification.setSubject("Password Reset Verification Code");
        notification.setBody(buildEmailBody(fullName, code));

        kafkaTemplate.send("email-notifications", notification);
        log.info("Verification code sent to email: {}", email);
    }

    /**
     * NEW METHOD: Send verification code via SMS
     */
    private void sendSmsVerificationCode(String phoneNumber, String code) {
        SmsNotificationDto notification = new SmsNotificationDto();
        notification.setPhoneNumber(phoneNumber);
        notification.setMessage("Your password reset verification code is: " + code +
                ". Valid for " + CODE_EXPIRY_MINUTES + " minutes.");
        notification.setProvider("SMS");

        kafkaTemplate.send("sms-notifications", notification);
        log.info("Verification code sent to phone: {}", phoneNumber);
    }

    /**
     * NEW METHOD: Send password reset confirmation email
     */
    private void sendPasswordResetConfirmation(String email, String fullName) {
        EmailNotificationDto notification = new EmailNotificationDto();
        notification.setRecipient(email);
        notification.setSubject("Password Reset Successful");
        notification.setBody(buildConfirmationEmailBody(fullName));

        kafkaTemplate.send("email-notifications", notification);
    }

    /**
     * NEW METHOD: Build email body for verification code
     */
    private String buildEmailBody(String fullName, String code) {
        return String.format("""
            <html>
            <body>
                <h2>Password Reset Request</h2>
                <p>Hello %s,</p>
                <p>You have requested to reset your password. Please use the following verification code:</p>
                <h1 style="color: #4F46E5; letter-spacing: 5px;">%s</h1>
                <p>This code will expire in %d minutes.</p>
                <p>If you did not request this password reset, please ignore this email.</p>
                <br>
                <p>Best regards,<br>Medical Consultation System Team</p>
            </body>
            </html>
            """, fullName, code, CODE_EXPIRY_MINUTES);
    }

    /**
     * NEW METHOD: Build confirmation email body
     */
    private String buildConfirmationEmailBody(String fullName) {
        return String.format("""
            <html>
            <body>
                <h2>Password Reset Successful</h2>
                <p>Hello %s,</p>
                <p>Your password has been successfully reset.</p>
                <p>If you did not make this change, please contact support immediately.</p>
                <br>
                <p>Best regards,<br>Medical Consultation System Team</p>
            </body>
            </html>
            """, fullName);
    }

    // ========== ADMIN USER MANAGEMENT METHODS ==========

    /**
     * Get user by ID
     */
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        return convertToUserDto(user);
    }

    /**
     * Update user status (ACTIVE, SUSPENDED, INACTIVE, etc.)
     */
    @Transactional
    public void updateUserStatus(Long userId, String status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        UserStatus newStatus;
        try {
            newStatus = UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status: " + status, HttpStatus.BAD_REQUEST);
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} status changed from {} to {}. Reason: {}",
                userId, oldStatus, newStatus, reason);

        // Send notification via Kafka
        try {
            EmailNotificationDto notification = new EmailNotificationDto();
            notification.setRecipient(user.getEmail());
            notification.setSubject("Account Status Update");

            if (newStatus == UserStatus.SUSPENDED) {
                notification.setBody(String.format(
                        "Dear %s,\n\nYour account has been suspended.\nReason: %s\n\n" +
                                "If you believe this is a mistake, please contact support.\n\n" +
                                "Best regards,\nMedical Consultation System",
                        user.getFullName(), reason
                ));
            } else if (newStatus == UserStatus.ACTIVE && oldStatus == UserStatus.SUSPENDED) {
                notification.setBody(String.format(
                        "Dear %s,\n\nYour account has been reactivated.\n" +
                                "You can now access your account normally.\n\n" +
                                "Best regards,\nMedical Consultation System",
                        user.getFullName()
                ));
            }

            authEventProducer.sendEmailNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send status update notification: {}", e.getMessage());
        }
    }

    /**
     * Delete user account permanently
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        // Prevent deletion of admin accounts (optional safety check)
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Cannot delete admin accounts", HttpStatus.FORBIDDEN);
        }

        log.info("Deleting user: {} ({})", user.getId(), user.getEmail());

        // Send final notification before deletion
        try {
            EmailNotificationDto notification = new EmailNotificationDto();
            notification.setRecipient(user.getEmail());
            notification.setSubject("Account Deleted");
            notification.setBody(String.format(
                    "Dear %s,\n\nYour account has been permanently deleted from our system.\n\n" +
                            "If you did not request this action, please contact support immediately.\n\n" +
                            "Best regards,\nMedical Consultation System",
                    user.getFullName()
            ));

            authEventProducer.sendEmailNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send deletion notification: {}", e.getMessage());
        }

        // Delete the user
        user.setIsDeleted(true);
        userRepository.save(user);

        log.info("User {} deleted successfully", userId);

        // TODO: Publish Kafka event for other services to clean up related data
        // Example: authEventProducer.sendUserDeletedEvent(userId, user.getRole());
    }

    /**
     * Admin-initiated password reset
     */
    @Transactional
    public void adminResetPassword(Long userId, String temporaryPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        // Hash the temporary password
        //String hashedPassword = passwordEncoder.encode(temporaryPassword);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset for user {} by admin", userId);

        // Send notification with temporary password
        try {
            EmailNotificationDto notification = new EmailNotificationDto();
            notification.setRecipient(user.getEmail());
            notification.setSubject("Password Reset by Administrator");
            notification.setBody(String.format(
                    "Dear %s,\n\nYour password has been reset by an administrator.\n\n" +
                            "Your temporary password is: %s\n\n" +
                            "Please log in and change your password immediately.\n\n" +
                            "Best regards,\nMedical Consultation System",
                    user.getFullName(), temporaryPassword
            ));

            authEventProducer.sendEmailNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send password reset notification: {}", e.getMessage());
        }
    }

    @Transactional
    public void verifyDoctorUser(String doctorEmail, String newStatus, Boolean approved){
        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new BusinessException("Doctor user not found", HttpStatus.NOT_FOUND));

        if( user.getStatus().equals(UserStatus.PENDING_VERIFICATION) && !user.getIsDeleted()  && approved ){
            user.setStatus(UserStatus.ACTIVE);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        log.info("Doctor {} verification status has been updated to: {}", doctorEmail, newStatus);
    }

    /**
     * Get user by email address (for inter-service communication)
     * Used by supervisor-service to check if patient account exists
     */
    public com.commonlibrary.entity.User getUserByEmail(String email) {
//        log.debug("Getting user by email: {}", email);
//        com.commonlibrary.entity.User user = userRepository.findByEmail(email).orElse(null);
//        return user;
        log.debug("Getting user by email: {}", email);
        com.authservice.entity.User authUser = userRepository.findByEmail(email).orElse(null);
        if (authUser == null) {
            return null;
        }
        // Map auth-service User to common-library User
        com.commonlibrary.entity.User commonUser = new com.commonlibrary.entity.User();

        commonUser.setId(authUser.getId());
        commonUser.setEmail(authUser.getEmail());
        commonUser.setFullName(authUser.getFullName());
        commonUser.setRole(authUser.getRole().toString());
        commonUser.setStatus(authUser.getStatus().toString());
        commonUser.setEmailVerified(authUser.getEmailVerified());
        commonUser.setCreatedAt(authUser.getCreatedAt());
        commonUser.setUpdatedAt(authUser.getUpdatedAt());
        commonUser.setLastLoginAt(authUser.getLastLogin());

        return commonUser;
    }

    /**
     * Create user account (for supervisor creating patient)
     * This allows supervisors to create patient accounts on their behalf
     */
    @Transactional
    public UserDto createUser(com.commonlibrary.dto.CreateUserRequest request) {
        log.info("Creating user account: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("User with this email already exists", HttpStatus.CONFLICT);
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.valueOf(request.getRole()))
                .status(UserStatus.ACTIVE) // Directly active since created by supervisor
                .emailVerified(false)
                .isDeleted(false)
                .timeZone("UTC")
                .preferredLanguage("EN")
                .build();

        User savedUser = userRepository.save(user);

        // Send Kafka event
//        authEventProducer.sendUserRegistrationEvent(
//                savedUser.getId(),
//                savedUser.getEmail(),
//                savedUser.getPhoneNumber(),
//                savedUser.getRole().name(),
//                savedUser.getFullName()
//        );

        log.info("User created successfully - userId: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        // Convert to DTO
        return convertToUserDto(savedUser);
    }
}
