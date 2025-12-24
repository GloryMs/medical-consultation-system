package com.authservice.controller;

import com.authservice.dto.*;
import com.authservice.service.AuthService;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.UserDto;
import com.commonlibrary.dto.UserStasDto;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.entity.UserStatus;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.authservice.dto.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Google login successful"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status)
    {
        Page<UserDto> users = authService.getAllUsers(page, size, role, status);
        return  ResponseEntity.ok(ApiResponse.success(users, "User list successful"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStasDto>> getUserStats(){
        UserStasDto userStasDto = authService.getUsersStats();
        return ResponseEntity.ok(ApiResponse.success(userStasDto, "User stats successful"));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        String message = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(message, "Verification code sent"));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<String>> verifyAndResetPassword(
            @Valid @RequestBody VerifyResetCodeRequest request) {
        String message = authService.verifyCodeAndResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message, "Password reset successful"));
    }

    /**
     * Get user by ID
     * GET /api/auth/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId) {
        UserDto user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    /**
     * Update user status (for admin operations like suspend/activate)
     * PUT /api/auth/users/{userId}/status
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status,
            @RequestParam String reason) {
        authService.updateUserStatus(userId, status, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "User status updated successfully"));
    }

    /**
     * Delete user account permanently
     * DELETE /api/auth/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    /**
     * Reset user password (admin initiated)
     * POST /api/auth/users/{userId}/reset-password
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable Long userId,
            @RequestParam String temporaryPassword) {
        authService.adminResetPassword(userId, temporaryPassword);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

}
