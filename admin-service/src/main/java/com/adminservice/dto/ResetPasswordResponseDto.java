package com.adminservice.dto;

import lombok.Data;

@Data
public class ResetPasswordResponseDto {
    private Long userId;
    private String temporaryPassword;
    private String message;
}