package com.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for verifying reset code and setting new password
 * Used when user submits the verification code along with new password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResetCodeRequest {
    
    /**
     * User's identifier (email or phone number)
     * Must match the identifier used in password reset request
     */
    @NotBlank(message = "Identifier is required")
    private String identifier;
    
    /**
     * 6-digit verification code sent to user
     * Must be exactly 6 digits
     */
    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    @Pattern(regexp = "\\d{6}", message = "Verification code must contain only digits")
    private String code;
    
    /**
     * New password chosen by user
     * Must meet password requirements:
     * - At least 8 characters
     * - Contains uppercase letter
     * - Contains lowercase letter
     * - Contains number
     * - Contains special character
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String newPassword;
}