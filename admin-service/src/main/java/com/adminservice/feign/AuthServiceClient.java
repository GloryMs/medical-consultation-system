package com.adminservice.feign;

import com.commonlibrary.dto.*;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.entity.UserStatus;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    
    @PutMapping("/api/auth/users/{userId}/status")
    void updateUserStatus(@PathVariable Long userId, 
                         @RequestParam String status,
                         @RequestParam String reason);
    
    @PostMapping("/api/auth/users/{userId}/reset-password")
    void resetPassword(@PathVariable Long userId, 
                      @RequestParam String temporaryPassword);

    @GetMapping("/api/auth/users")
    ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) UserRole role,
                                                           @RequestParam(required = false) UserStatus status);

    @GetMapping("/api/auth/stats")
    ResponseEntity<ApiResponse<UserStatsDto>> getUsersStats();

    /**
     * Get user by ID
     */
    @GetMapping("/api/auth/users/{userId}")
    ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId);

    /**
     * Delete user account permanently
     */
    @DeleteMapping("/api/auth/users/{userId}")
    void deleteUser(@PathVariable Long userId);


}