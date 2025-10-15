package com.patientservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.patientservice.dto.CreateDependentDto;
import com.patientservice.dto.DependentDto;
import com.patientservice.dto.UpdateDependentDto;
import com.patientservice.service.DependentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/dependents")
@RequiredArgsConstructor
@Slf4j
public class DependentController {

    private final DependentService dependentService;

    /**
     * Create a new dependent
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DependentDto>> createDependent(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateDependentDto dto) {
        
        log.info("Creating dependent for user: {}", userId);
        DependentDto dependent = dependentService.createDependent(userId, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dependent, "Dependent created successfully"));
    }

    /**
     * Get all dependents for the authenticated patient
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DependentDto>>> getDependents(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Fetching dependents for user: {}", userId);
        List<DependentDto> dependents = dependentService.getDependents(userId);
        
        return ResponseEntity.ok(ApiResponse.success(dependents, "Dependents retrieved successfully"));
    }

    /**
     * Get a specific dependent by ID
     */
    @GetMapping("/{dependentId}")
    public ResponseEntity<ApiResponse<DependentDto>> getDependent(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long dependentId) {
        
        log.info("Fetching dependent {} for user {}", dependentId, userId);
        DependentDto dependent = dependentService.getDependent(userId, dependentId);
        
        return ResponseEntity.ok(ApiResponse.success(dependent, "Dependent retrieved successfully"));
    }

    /**
     * Update a dependent
     */
    @PutMapping("/{dependentId}")
    public ResponseEntity<ApiResponse<DependentDto>> updateDependent(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long dependentId,
            @Valid @RequestBody UpdateDependentDto dto) {
        
        log.info("Updating dependent {} for user {}", dependentId, userId);
        DependentDto dependent = dependentService.updateDependent(userId, dependentId, dto);
        
        return ResponseEntity.ok(ApiResponse.success(dependent, "Dependent updated successfully"));
    }

    /**
     * Delete a dependent (soft delete)
     */
    @DeleteMapping("/{dependentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependent(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long dependentId) {
        
        log.info("Deleting dependent {} for user {}", dependentId, userId);
        dependentService.deleteDependent(userId, dependentId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Dependent deleted successfully"));
    }
}