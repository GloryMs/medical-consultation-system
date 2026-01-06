package com.supervisorservice.controller;

import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CreateCaseDto;
import com.supervisorservice.dto.ApiResponse;
import com.supervisorservice.dto.CreateCaseRequest;
import com.supervisorservice.dto.UpdateCaseDto;
import com.supervisorservice.service.CaseManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for supervisor case management operations
 */
@RestController
@RequestMapping("/api/supervisors/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Case Management", description = "Case submission and management on behalf of patients")
public class SupervisorCaseController {
    
    private final CaseManagementService caseManagementService;
    
    /**
     * Submit a case on behalf of patient
     */
    @PostMapping("/patient/{patientId}")
    @Operation(summary = "Submit case for patient", 
               description = "Submits a medical case on behalf of an assigned patient")
    public ResponseEntity<ApiResponse<CaseDto>> submitCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long patientId,
            @Valid @RequestBody CreateCaseDto request) {
        
        log.info("POST /api/supervisors/cases/patient/{} - userId: {}", patientId, userId);
        
        CaseDto caseDto = caseManagementService.submitCase(userId, patientId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case submitted successfully", caseDto));
    }
    
    /**
     * Get all cases for supervisor's patients
     */
    @GetMapping
    @Operation(summary = "Get all cases", 
               description = "Retrieves all cases for patients under supervisor's care")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAllCases(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/cases - userId: {}", userId);
        
        List<CaseDto> cases = caseManagementService.getCasesForSupervisor(userId);
        
        return ResponseEntity.ok(ApiResponse.success(cases));
    }
    
    /**
     * Get cases for specific patient
     */
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient cases", 
               description = "Retrieves all cases for a specific patient")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getPatientCases(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long patientId) {
        
        log.info("GET /api/supervisors/cases/patient/{} - userId: {}", patientId, userId);
        
        List<CaseDto> cases = caseManagementService.getCasesForPatient(userId, patientId);
        
        return ResponseEntity.ok(ApiResponse.success(cases));
    }
    
    /**
     * Get specific case details
     */
    @GetMapping("/{caseId}")
    @Operation(summary = "Get case details", 
               description = "Retrieves detailed information about a specific case")
    public ResponseEntity<ApiResponse<CaseDto>> getCaseDetails(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId) {
        
        log.info("GET /api/supervisors/cases/{} - userId: {}", caseId, userId);
        
        CaseDto caseDto = caseManagementService.getCaseDetails(userId, caseId);
        
        return ResponseEntity.ok(ApiResponse.success(caseDto));
    }
    
    /**
     * Update case information
     */
    @PutMapping("/{caseId}")
    @Operation(summary = "Update case", 
               description = "Updates case information before it's assigned to a doctor")
    public ResponseEntity<ApiResponse<CaseDto>> updateCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody UpdateCaseDto request) {
        
        log.info("PUT /api/supervisors/cases/{} - userId: {}", caseId, userId);
        
        CaseDto caseDto = caseManagementService.updateCase(userId, caseId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Case updated successfully", caseDto));
    }
    
    /**
     * Cancel case
     */
    @PutMapping("/{caseId}/cancel")
    @Operation(summary = "Cancel case", 
               description = "Cancels a case before it's assigned to a doctor")
    public ResponseEntity<ApiResponse<Void>> cancelCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @RequestParam String reason) {
        
        log.info("PUT /api/supervisors/cases/{}/cancel - userId: {}, reason: {}", caseId, userId, reason);
        
        caseManagementService.cancelCase(userId, caseId, reason);
        
        return ResponseEntity.ok(ApiResponse.success("Case cancelled successfully", null));
    }
    
    /**
     * Get patient information
     */
    @GetMapping("/patient/{patientId}/info")
    @Operation(summary = "Get patient info",
               description = "Retrieves patient information for case submission")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatientInfo(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long patientId) {

        log.info("GET /api/supervisors/cases/patient/{}/info - userId: {}", patientId, userId);

        Map<String, Object> patientInfo = caseManagementService.getPatientInfo(userId, patientId);

        return ResponseEntity.ok(ApiResponse.success(patientInfo));
    }
}