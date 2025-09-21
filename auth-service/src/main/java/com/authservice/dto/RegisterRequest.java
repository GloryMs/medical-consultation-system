package com.authservice.dto;

import com.commonlibrary.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private UserRole role;
    private String fullName;
    private String phoneNumber;
}
