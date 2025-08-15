package com.authservice.dto;

import com.commonlibrary.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;

    @NotNull(message = "Role is required")
    private UserRole role;
}
