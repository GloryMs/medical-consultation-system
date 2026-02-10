package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CreateUserRequest;
import com.commonlibrary.dto.UserDto;
import com.commonlibrary.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Feign client for Auth Service
 */
@FeignClient(name = "auth-service"
//        , url = "${auth-service.url:http://localhost:8081}"
)
public interface AuthServiceClient {

    /**
     * Get user by email address
     * @param email User's email address
     * @return ApiResponse with User if exists
     */
    @GetMapping("/api/auth/users/by-email")
    ResponseEntity<ApiResponse<User>> getUserByEmail(@RequestParam("email") String email);

    /**
     * Create a new user account
     * @param request User creation request
     * @return ApiResponse with created user DTO
     */
    @PostMapping("/api/auth/users/create")
    ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody CreateUserRequest request);
}