package com.supervisorservice.service;

import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CreateCaseDto;
import com.commonlibrary.entity.CaseStatus;
import com.supervisorservice.dto.CreateCaseRequest;
import com.supervisorservice.dto.UpdateCaseDto;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.exception.CaseLimitExceededException;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for managing cases on behalf of patients
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseManagementService {

    private final PatientServiceClient patientServiceClient;
    private final SupervisorValidationService validationService;
    private final SupervisorKafkaProducer kafkaProducer;
    private final SupervisorPatientAssignmentRepository assignmentRepository;

    /**
     * Submit a case on behalf of a patient
     */
    @Transactional
    public CaseDto submitCase(Long userId, Long patientId, CreateCaseDto request) {
        log.info("Submitting case for patient: {} by supervisor userId: {}", patientId, userId);

        // Validate supervisor is active and has access to patient
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        //update request with supervisorId:
        request.setSupervisorId(supervisor.getId());
        validationService.validatePatientAccess(supervisor.getId(), patientId);

        // Get patient's userId (needed because patient-service expects patient's userId, not supervisor's)
        Map<String, Object> patientInfo = patientServiceClient.getPatientBasicInfo(patientId).getData();
        if (patientInfo == null || !patientInfo.containsKey("userId")) {
            throw new RuntimeException("Unable to retrieve patient information. Patient userId not found.");
        }

        Long patientUserId = convertToLong(patientInfo.get("userId"));
        if (patientUserId == null) {
            throw new RuntimeException("Invalid patient userId retrieved from patient-service.");
        }

        log.debug("Retrieved patient userId: {} for patientId: {}", patientUserId, patientId);

        // Check active case limit for this patient
        List<CaseDto> cases = patientServiceClient.getAllCasesForAdmin(
                null, null, null, patientId, null, null, null, null).getData();

        long activeCasesCount = cases.stream()
                .filter(c -> c.getStatus() == CaseStatus.PENDING ||
                        c.getStatus() == CaseStatus.ASSIGNED ||
                        c.getStatus() == CaseStatus.IN_PROGRESS)
                .count();

        if (activeCasesCount >= supervisor.getMaxActiveCasesPerPatient()) {
            throw new CaseLimitExceededException(
                    String.format("Patient has reached the maximum active cases limit (%d/%d). " +
                            "Please wait for existing cases to be completed before submitting new ones.",
                            activeCasesCount, supervisor.getMaxActiveCasesPerPatient()));
        }

        // Submit case via patient-service using patient's userId (not supervisor's userId)
        // Note: This allows the case to be created under the patient's account
        CaseDto caseDto = patientServiceClient.createCaseJson( patientUserId, request).getBody().getData();

        log.info("Case submitted successfully - caseId: {}, patientId: {}, submitted by supervisor userId: {}",
                caseDto.getId(), patientId, userId);

        // Publish event (using supervisor's ID for audit trail)
        kafkaProducer.sendCaseSubmittedEvent(supervisor.getId(), patientId, caseDto.getId());

        return caseDto;
    }

    /**
     * Helper method to safely convert Object to Long
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get all cases for patients under supervisor's care
     */
    @Transactional(readOnly = true)
    public List<CaseDto> getCasesForSupervisor(Long userId) {
        log.debug("Getting all cases for supervisor userId: {}", userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        // Get all patients assigned to this supervisor
        List<Long> patientIds = getPatientIds(supervisor.getId());

        // Fetch cases for all patients
        return patientIds.stream()
                .flatMap(patientId -> {
                    try {
                        List<CaseDto> patientCases = patientServiceClient.getAllCasesForAdmin(
                                null, null, null, patientId, null, null, null, null).getData();
                        return patientCases != null ? patientCases.stream() : java.util.stream.Stream.empty();
                    } catch (Exception e) {
                        log.error("Error fetching cases for patient: {}", patientId, e);
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }

    /**
     * Get cases for a specific patient
     */
    @Transactional(readOnly = true)
    public List<CaseDto> getCasesForPatient(Long userId, Long patientId) {
        log.debug("Getting cases for patient: {} by supervisor userId: {}", patientId, userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        validationService.validatePatientAccess(supervisor.getId(), patientId);

        return patientServiceClient.getAllCasesForAdmin(
                null, null, null, patientId, null, null, null, null).getData();
    }

    /**
     * Get specific case details
     */
    @Transactional(readOnly = true)
    public CaseDto getCaseDetails(Long userId, Long caseId) {
        log.debug("Getting case details - caseId: {}, supervisor userId: {}", caseId, userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        CaseDto caseDto = patientServiceClient.getCaseDetails(caseId).getData();

        // Validate supervisor has access to this patient
        validationService.validatePatientAccess(supervisor.getId(), caseDto.getPatientId());

        return caseDto;
    }

    /**
     * Update case information
     */
    @Transactional
    public CaseDto updateCase(Long userId, Long caseId, UpdateCaseDto request) {
        log.info("Updating case - caseId: {}, supervisor userId: {}", caseId, userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        // Get case to verify patient access
        CaseDto existingCase = patientServiceClient.getCaseDetails(caseId).getData();
        validationService.validatePatientAccess(supervisor.getId(), existingCase.getPatientId());

        // Update case via patient-service
        patientServiceClient.updateCase(caseId, request);

        // Fetch updated case
        CaseDto updatedCase = patientServiceClient.getCaseDetails(caseId).getData();

        log.info("Case updated successfully - caseId: {}", caseId);

        return updatedCase;
    }

    /**
     * Cancel a case (delete)
     */
    @Transactional
    public void cancelCase(Long userId, Long caseId, String reason) {
        log.info("Cancelling case - caseId: {}, supervisor userId: {}, reason: {}", caseId, userId, reason);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        // Get case to verify patient access
        CaseDto existingCase = patientServiceClient.getCaseDetails(caseId).getData();
        validationService.validatePatientAccess(supervisor.getId(), existingCase.getPatientId());

        // Get patient's userId (needed because patient-service expects patient's userId, not supervisor's)
        Map<String, Object> patientInfo = patientServiceClient.getPatientBasicInfo(existingCase.getPatientId()).getData();
        if (patientInfo == null || !patientInfo.containsKey("userId")) {
            throw new RuntimeException("Unable to retrieve patient information. Patient userId not found.");
        }

        Long patientUserId = convertToLong(patientInfo.get("userId"));
        if (patientUserId == null) {
            throw new RuntimeException("Invalid patient userId retrieved from patient-service.");
        }

        log.debug("Retrieved patient userId: {} for patientId: {}", patientUserId, existingCase.getPatientId());

        // Delete case via patient-service
        patientServiceClient.deleteCase(caseId, patientUserId);

        log.info("Case cancelled successfully - caseId: {}", caseId);
    }

    /**
     * Get patient information
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPatientInfo(Long userId, Long patientId) {
        log.debug("Getting patient info - patientId: {}, supervisor userId: {}", patientId, userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        validationService.validatePatientAccess(supervisor.getId(), patientId);

        return patientServiceClient.getPatientBasicInfo(patientId).getData();
    }

    /**
     * Helper method to get patient IDs for a supervisor
     */
    private List<Long> getPatientIds(Long supervisorId) {
        return assignmentRepository.findPatientIdsBySupervisor(supervisorId);
    }
}