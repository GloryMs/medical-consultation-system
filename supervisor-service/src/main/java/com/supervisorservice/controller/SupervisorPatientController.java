package com.supervisorservice.controller;

import com.commonlibrary.dto.CreatePatientProfileRequest;
import com.commonlibrary.dto.CustomPatientDto;
import com.supervisorservice.dto.ApiResponse;
import com.supervisorservice.dto.AssignPatientByEmailRequest;
import com.supervisorservice.dto.PatientAssignmentDto;
import com.supervisorservice.service.PatientManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for patient management operations
 */
@RestController
@RequestMapping("/api/supervisors/patients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Patient Management", description = "Patient assignment management endpoints")
public class SupervisorPatientController {
    
    private final PatientManagementService patientService;
    
    /**
     * Get all assigned patients
     */
    @GetMapping
    @Operation(summary = "Get assigned patients", 
               description = "Retrieves all patients assigned to the supervisor")
    public ResponseEntity<ApiResponse<List<PatientAssignmentDto>>> getAssignedPatients(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/patients - userId: {}", userId);
        
        List<PatientAssignmentDto> patients = patientService.getAssignedPatients(userId);
        
        return ResponseEntity.ok(ApiResponse.success(patients));
    }
    
    /**
     * Assign patient to supervisor
     */
    @PostMapping
    @Operation(summary = "Assign patient", 
               description = "Assigns a patient to the supervisor")
    public ResponseEntity<ApiResponse<PatientAssignmentDto>> assignPatient(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long patientId,
            @RequestParam(required = false) String notes) {
        
        log.info("POST /api/supervisors/patients - userId: {}, patientId: {}", userId, patientId);
        
        PatientAssignmentDto assignment = patientService.assignPatient(userId, patientId, notes);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient assigned successfully", assignment));
    }
    
    /**
     * Get patient assignment details
     */
    @GetMapping("/{patientId}")
    @Operation(summary = "Get patient assignment", 
               description = "Retrieves specific patient assignment details")
    public ResponseEntity<ApiResponse<PatientAssignmentDto>> getPatientAssignment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long patientId) {
        
        log.info("GET /api/supervisors/patients/{} - userId: {}", patientId, userId);
        
        PatientAssignmentDto assignment = patientService.getPatientAssignment(userId, patientId);
        
        return ResponseEntity.ok(ApiResponse.success(assignment));
    }
    
    /**
     * Remove patient assignment
     */
    @DeleteMapping("/{patientId}")
    @Operation(summary = "Remove patient assignment", 
               description = "Removes patient assignment from supervisor")
    public ResponseEntity<ApiResponse<Void>> removePatient(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long patientId,
            @RequestParam(required = false) String reason) {
        
        log.info("DELETE /api/supervisors/patients/{} - userId: {}", patientId, userId);
        
        patientService.removePatientAssignment(userId, patientId, reason);
        
        return ResponseEntity.ok(ApiResponse.success("Patient assignment removed successfully", null));
    }
    
    /**
     * Get patient IDs
     */
    @GetMapping("/ids")
    @Operation(summary = "Get patient IDs", 
               description = "Retrieves list of patient IDs assigned to supervisor")
    public ResponseEntity<ApiResponse<List<Long>>> getPatientIds(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/patients/ids - userId: {}", userId);
        
        List<Long> patientIds = patientService.getAssignedPatientIds(userId);
        
        return ResponseEntity.ok(ApiResponse.success(patientIds));
    }


    /**
     * Get patient UserIDs
     */
    @GetMapping("/userIds")
    @Operation(summary = "Get patient IDs",
            description = "Retrieves list of patient IDs assigned to supervisor")
    public ResponseEntity<ApiResponse<List<Long>>> getPatientUserIds(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("GET /api/supervisors/patients/userIds - userId: {}", userId);

        List<Long> patientUserIds = patientService.getAssignedPatientUserIds(userId);

        return ResponseEntity.ok(ApiResponse.success(patientUserIds));
    }

    /**
     * OPTION 1: Create new patient profile and assign to supervisor
     */
    @PostMapping("/create-and-assign")
    @Operation(summary = "Create patient and assign",
            description = "Creates a new patient account and assigns to supervisor")
    public ResponseEntity<ApiResponse<PatientAssignmentDto>> createAndAssignPatient(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreatePatientProfileRequest request) {

        log.info("POST /api/supervisors/patients/create-and-assign - userId: {}", userId);

        PatientAssignmentDto assignment = patientService.createAndAssignPatient(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created and assigned successfully", assignment));
    }

    /**
     * OPTION 2: Assign existing patient by email
     */
    @PostMapping("/assign-by-email")
    @Operation(summary = "Assign existing patient by email",
            description = "Assigns an existing patient to supervisor using patient's email")
    public ResponseEntity<ApiResponse<PatientAssignmentDto>> assignPatientByEmail(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AssignPatientByEmailRequest request) {

        log.info("POST /api/supervisors/patients/assign-by-email - userId: {}, email: {}",
                userId, request.getPatientEmail());

        PatientAssignmentDto assignment = patientService.assignExistingPatientByEmail(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient assigned successfully", assignment));
    }

    @GetMapping ("/{caseId}/patient")
    public ResponseEntity<com.commonlibrary.dto.ApiResponse<CustomPatientDto>>
    getCustomPatientInfo(@PathVariable Long caseId, @RequestParam Long patientId,
                         @RequestHeader("X-User-Id") Long userId){
        //
        CustomPatientDto customPatientInfo = patientService.getCustomPatientInfo(caseId, userId, patientId);
        return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.success(customPatientInfo));
    }

}
