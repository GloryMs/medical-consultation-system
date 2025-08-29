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
    public ResponseEntity<ApiResponse<UserStasDto>> getUserStas(){
        UserStasDto userStasDto = authService.getUsersStats();
        return ResponseEntity.ok(ApiResponse.success(userStasDto, "User stats successful"));
    }
}
