package com.adminservice.service;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseAnalyticsDto;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CaseMetricsDto;
import com.commonlibrary.entity.AssignmentPriority;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.exception.BusinessException;
import com.adminservice.dto.AssignCaseRequest;
import com.adminservice.dto.CaseFilterDto;
import com.adminservice.feign.DoctorServiceClient;
import com.adminservice.feign.PatientServiceClient;
import com.adminservice.feign.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminCaseService {

    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    /**
     * Get all cases with filtering and pagination
     */
    public Page<CaseDto> getAllCases(CaseFilterDto filterDto, Pageable pageable) {
        try {
            log.info("Fetching cases with filters: {}", filterDto);
            
            // Call patient-service to get all cases
            ApiResponse<List<CaseDto>> response = patientServiceClient.getAllCasesForAdmin(
                    filterDto.getStatus(),
                    filterDto.getUrgencyLevel(),
                    filterDto.getSpecialization(),
                    filterDto.getPatientId(),
                    filterDto.getDoctorId(),
                    filterDto.getStartDate(),
                    filterDto.getEndDate(),
                    filterDto.getSearchTerm()
            ).getBody();
            
            if (response == null || response.getData() == null) {
                return Page.empty(pageable);
            }
            
            List<CaseDto> allCases = response.getData();
            
            // Enrich cases with patient and doctor names
            enrichCasesWithUserInfo(allCases);
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allCases.size());
            
            List<CaseDto> pagedCases = allCases.subList(start, end);
            
            return new PageImpl<>(pagedCases, pageable, allCases.size());
            
        } catch (Exception e) {
            log.error("Error fetching cases: {}", e.getMessage(), e);
            throw new BusinessException("Failed to fetch cases", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case by ID with full details
     */
    public CaseDto getCaseById(Long caseId) {
        try {
            log.info("Fetching case details for caseId: {}", caseId);
            
            ApiResponse<CaseDto> response = patientServiceClient.getCaseById(caseId).getBody();
            
            if (response == null || response.getData() == null) {
                throw new BusinessException("Case not found", HttpStatus.NOT_FOUND);
            }
            
            CaseDto caseDto = response.getData();
            
            // Enrich with patient and doctor details
            enrichCaseWithUserInfo(caseDto);
            
            // Get assignment history
            try {
                ApiResponse<List<?>> assignmentResponse = patientServiceClient
                        .getCaseAssignmentHistory(caseId).getBody();
                if (assignmentResponse != null && assignmentResponse.getData() != null) {
                    // Store assignment history in a custom field if needed
                    log.info("Retrieved {} assignment records for case {}", 
                            assignmentResponse.getData().size(), caseId);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch assignment history: {}", e.getMessage());
            }
            
            return caseDto;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching case {}: {}", caseId, e.getMessage(), e);
            throw new BusinessException("Failed to fetch case details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assign case to doctor
     */
    public void assignCaseToDoctor(Long caseId, Long doctorId, Long adminUserId) {
        try {
            log.info("Assigning case {} to doctor {} by admin {}", caseId, doctorId, adminUserId);
            
            // Validate case exists and is in valid status
            CaseDto caseDto = getCaseById(caseId);
            
            if (caseDto.getStatus() != CaseStatus.PENDING && 
                caseDto.getStatus() != CaseStatus.SUBMITTED) {
                throw new BusinessException(
                        "Case is not in PENDING or SUBMITTED status. Current status: " + caseDto.getStatus(),
                        HttpStatus.BAD_REQUEST);
            }
            
            // Validate doctor exists and is verified
            validateDoctor(doctorId, caseDto.getRequiredSpecialization());
            
            // Create assignment via patient-service
            Map<String, Object> assignmentRequest = new HashMap<>();
            assignmentRequest.put("caseId", caseId);
            assignmentRequest.put("doctorId", doctorId);
            assignmentRequest.put("priority", AssignmentPriority.PRIMARY.name());
            assignmentRequest.put("assignedBy", "ADMIN");
            assignmentRequest.put("adminUserId", adminUserId);
            
            ApiResponse<?> response = patientServiceClient
                    .createCaseAssignment(assignmentRequest).getBody();
            
            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Failed to create case assignment", 
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Send notification to doctor
            sendAssignmentNotification(caseId, doctorId, caseDto);
            
            log.info("Successfully assigned case {} to doctor {}", caseId, doctorId);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error assigning case: {}", e.getMessage(), e);
            throw new BusinessException("Failed to assign case", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reassign case to different doctor
     */
    public void reassignCase(Long caseId, Long newDoctorId, String reason, Long adminUserId) {
        try {
            log.info("Reassigning case {} to new doctor {} - Reason: {}", 
                     caseId, newDoctorId, reason);
            
            // Get current case details
            CaseDto caseDto = getCaseById(caseId);
            
            // Validate case can be reassigned
            if (caseDto.getStatus() == CaseStatus.CLOSED || 
                caseDto.getStatus() == CaseStatus.CONSULTATION_COMPLETE) {
                throw new BusinessException("Cannot reassign completed or closed cases", 
                        HttpStatus.BAD_REQUEST);
            }
            
            // Validate new doctor
            validateDoctor(newDoctorId, caseDto.getRequiredSpecialization());
            
            // Get current assignment to mark as reassigned
            Long currentDoctorId = getCurrentAssignedDoctorId(caseId);
            
            if (currentDoctorId != null && currentDoctorId.equals(newDoctorId)) {
                throw new BusinessException("Case is already assigned to this doctor", 
                        HttpStatus.BAD_REQUEST);
            }
            
            // Mark current assignment as REASSIGNED (no such status so => PENDING).
            if (currentDoctorId != null) {
                updateAssignmentStatus(caseId, currentDoctorId, AssignmentStatus.PENDING);
            }
            
            // Create new assignment
            Map<String, Object> assignmentRequest = new HashMap<>();
            assignmentRequest.put("caseId", caseId);
            assignmentRequest.put("doctorId", newDoctorId);
            assignmentRequest.put("priority", AssignmentPriority.PRIMARY.name());
            assignmentRequest.put("assignedBy", "ADMIN");
            assignmentRequest.put("adminUserId", adminUserId);
            assignmentRequest.put("reassignmentReason", reason);
            
            ApiResponse<?> response = patientServiceClient
                    .createCaseAssignment(assignmentRequest).getBody();
            
            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Failed to create new assignment", 
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Send notifications
            if (currentDoctorId != null) {
                sendReassignmentNotificationToOldDoctor(caseId, currentDoctorId, reason);
            }
            sendAssignmentNotification(caseId, newDoctorId, caseDto);
            
            log.info("Successfully reassigned case {} to doctor {}", caseId, newDoctorId);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error reassigning case: {}", e.getMessage(), e);
            throw new BusinessException("Failed to reassign case", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update case status (admin override)
     */
    public void updateCaseStatus(Long caseId, CaseStatus newStatus, String reason, Long adminUserId) {
        try {
            log.info("Admin {} updating case {} status to {} - Reason: {}", 
                     adminUserId, caseId, newStatus, reason);
            
            // Validate case exists
            CaseDto caseDto = getCaseById(caseId);
            
            // Update status via patient-service
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("status", newStatus.name());
            statusUpdate.put("notes", reason);
            statusUpdate.put("updatedBy", "ADMIN");
            statusUpdate.put("adminUserId", adminUserId);
            
            ApiResponse<?> response = patientServiceClient
                    .updateCaseStatus(caseId, statusUpdate).getBody();
            
            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Failed to update case status", 
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Send notification to patient
            sendStatusUpdateNotification(caseId, caseDto.getPatientId(), newStatus, reason);
            
            log.info("Successfully updated case {} status to {}", caseId, newStatus);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating case status: {}", e.getMessage(), e);
            throw new BusinessException("Failed to update case status", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case metrics
     */
    public CaseMetricsDto getCaseMetrics(String startDate, String endDate) {
        try {
            log.info("Fetching case metrics from {} to {}", startDate, endDate);
            
            ApiResponse<CaseMetricsDto> response = patientServiceClient
                    .getCaseMetrics(startDate, endDate).getBody();
            
            if (response == null || response.getData() == null) {
                return new CaseMetricsDto(); // Return empty metrics
            }
            
            return response.getData();
            
        } catch (Exception e) {
            log.error("Error fetching case metrics: {}", e.getMessage(), e);
            throw new BusinessException("Failed to fetch case metrics", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case assignment history
     */
    public List<?> getCaseAssignmentHistory(Long caseId) {
        try {
            ApiResponse<List<?>> response = patientServiceClient
                    .getCaseAssignmentHistory(caseId).getBody();
            
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            
            return response.getData();
            
        } catch (Exception e) {
            log.error("Error fetching assignment history: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get unassigned cases
     */
    public List<CaseDto> getUnassignedCases(String urgencyLevel, String specialization) {
        try {
            ApiResponse<List<CaseDto>> response = patientServiceClient
                    .getUnassignedCases(urgencyLevel, specialization).getBody();
            
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            
            List<CaseDto> cases = response.getData();
            enrichCasesWithUserInfo(cases);
            
            return cases;
            
        } catch (Exception e) {
            log.error("Error fetching unassigned cases: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get overdue cases
     */
    public List<CaseDto> getOverdueCases() {
        try {
            ApiResponse<List<CaseDto>> response = patientServiceClient
                    .getOverdueCases().getBody();
            
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            
            List<CaseDto> cases = response.getData();
            enrichCasesWithUserInfo(cases);
            
            return cases;
            
        } catch (Exception e) {
            log.error("Error fetching overdue cases: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Bulk assign cases
     */
    public void bulkAssignCases(List<AssignCaseRequest> assignments, Long adminUserId) {
        int successCount = 0;
        int failureCount = 0;
        
        for (AssignCaseRequest assignment : assignments) {
            try {
                assignCaseToDoctor(assignment.getCaseId(), assignment.getDoctorId(), adminUserId);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to assign case {}: {}", assignment.getCaseId(), e.getMessage());
                failureCount++;
            }
        }
        
        log.info("Bulk assignment completed - Success: {}, Failed: {}", successCount, failureCount);
        
        if (failureCount > 0 && successCount == 0) {
            throw new BusinessException("All assignments failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Enrich cases with patient and doctor information
     */
    private void enrichCasesWithUserInfo(List<CaseDto> cases) {
        for (CaseDto caseDto : cases) {
            enrichCaseWithUserInfo(caseDto);
        }
    }

    /**
     * Enrich single case with user information
     */
    private void enrichCaseWithUserInfo(CaseDto caseDto) {
        try {
            // Get patient name
            if (caseDto.getPatientId() != null) {
                try {
                    ApiResponse<?> patientResponse = patientServiceClient
                            .getPatientBasicInfo(caseDto.getPatientId()).getBody();
                    if (patientResponse != null && patientResponse.getData() != null) {
                        Map<String, Object> patientData = (Map<String, Object>) patientResponse.getData();
                        caseDto.setPatientName((String) patientData.get("fullName"));
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch patient name: {}", e.getMessage());
                }
            }
            
            // Get doctor name if assigned
            Long doctorId = getCurrentAssignedDoctorId(caseDto.getId());
            if (doctorId != null) {
                try {
                    ApiResponse<?> doctorResponse = doctorServiceClient
                            .getDoctorBasicInfo(doctorId).getBody();
                    if (doctorResponse != null && doctorResponse.getData() != null) {
                        Map<String, Object> doctorData = (Map<String, Object>) doctorResponse.getData();
                        caseDto.setDoctorName((String) doctorData.get("fullName"));
                        caseDto.setAssignedDoctorId(doctorId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch doctor name: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error enriching case {}: {}", caseDto.getId(), e.getMessage());
        }
    }

    /**
     * Validate doctor eligibility
     */
    private void validateDoctor(Long doctorId, String requiredSpecialization) {
        try {
            ApiResponse<?> response = doctorServiceClient.getDoctorById(doctorId).getBody();
            
            if (response == null || response.getData() == null) {
                throw new BusinessException("Doctor not found", HttpStatus.NOT_FOUND);
            }
            
            Map<String, Object> doctorData = (Map<String, Object>) response.getData();
            
            // Check verification status
            String verificationStatus = (String) doctorData.get("verificationStatus");
            if (!"VERIFIED".equals(verificationStatus)) {
                throw new BusinessException("Doctor is not verified", HttpStatus.BAD_REQUEST);
            }
            
            // Check specialization match
            String primarySpecialization = (String) doctorData.get("primarySpecialization");
            List<String> secondarySpecializations = (List<String>) doctorData.get("secondarySpecializations");
            
            boolean specializationMatch = primarySpecialization != null && 
                    primarySpecialization.equals(requiredSpecialization);
            
            if (!specializationMatch && secondarySpecializations != null) {
                specializationMatch = secondarySpecializations.contains(requiredSpecialization);
            }
            
            if (!specializationMatch) {
                log.warn("Doctor {} specialization does not match case requirement: {}", 
                        doctorId, requiredSpecialization);
                // Note: We log warning but don't throw exception to allow admin override
            }
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating doctor: {}", e.getMessage(), e);
            throw new BusinessException("Failed to validate doctor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public CaseAnalyticsDto getCaseAnalytics(String startDate, String endDate) {
        try {
            ApiResponse<CaseAnalyticsDto> response = patientServiceClient
                    .getCaseAnalytics(startDate, endDate)
                    .getBody();

            return response != null ? response.getData() : new CaseAnalyticsDto();
        } catch (Exception e) {
            log.error("Failed to fetch analytics: {}", e.getMessage());
            throw new BusinessException("Failed to fetch case analytics",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get currently assigned doctor ID for a case
     */
    private Long getCurrentAssignedDoctorId(Long caseId) {
        try {
            ApiResponse<List<?>> response = patientServiceClient
                    .getCaseAssignmentHistory(caseId).getBody();
            
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                // Get the most recent ACCEPTED or PENDING assignment
                for (Object assignmentObj : response.getData()) {
                    Map<String, Object> assignment = (Map<String, Object>) assignmentObj;
                    String status = (String) assignment.get("status");
                    if ("ACCEPTED".equals(status) || "PENDING".equals(status)) {
                        return ((Number) assignment.get("doctorId")).longValue();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error getting current doctor assignment: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Update assignment status
     */
    private void updateAssignmentStatus(Long caseId, Long doctorId, AssignmentStatus newStatus) {
        try {
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("caseId", caseId);
            updateRequest.put("doctorId", doctorId);
            updateRequest.put("status", newStatus.name());
            
            patientServiceClient.updateAssignmentStatus(updateRequest);
            
        } catch (Exception e) {
            log.error("Error updating assignment status: {}", e.getMessage(), e);
        }
    }

    /**
     * Send assignment notification to doctor
     */
    private void sendAssignmentNotification(Long caseId, Long doctorId, CaseDto caseDto) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", doctorId);
            notification.put("type", "CASE_ASSIGNED");
            notification.put("title", "New Case Assigned");
            notification.put("message", String.format("You have been assigned case #%d: %s", 
                    caseId, caseDto.getCaseTitle()));
            notification.put("priority", "HIGH");
            notification.put("caseId", caseId);
            
            //notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Error sending assignment notification: {}", e.getMessage());
        }
    }

    /**
     * Send reassignment notification to old doctor
     */
    private void sendReassignmentNotificationToOldDoctor(Long caseId, Long doctorId, String reason) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", doctorId);
            notification.put("type", "CASE_REASSIGNED");
            notification.put("title", "Case Reassigned");
            notification.put("message", String.format("Case #%d has been reassigned. Reason: %s", 
                    caseId, reason));
            notification.put("priority", "MEDIUM");
            notification.put("caseId", caseId);
            
            //notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Error sending reassignment notification: {}", e.getMessage());
        }
    }

    /**
     * Send status update notification to patient
     */
    private void sendStatusUpdateNotification(Long caseId, Long patientId, 
                                              CaseStatus newStatus, String reason) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", patientId);
            notification.put("type", "CASE_STATUS_UPDATE");
            notification.put("title", "Case Status Updated");
            notification.put("message", String.format("Your case #%d status has been updated to %s. %s", 
                    caseId, newStatus, reason != null ? reason : ""));
            notification.put("priority", "MEDIUM");
            notification.put("caseId", caseId);
            
            //notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Error sending status update notification: {}", e.getMessage());
        }
    }
}