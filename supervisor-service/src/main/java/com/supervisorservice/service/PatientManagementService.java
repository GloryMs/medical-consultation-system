package com.supervisorservice.service;

import com.commonlibrary.dto.CreatePatientProfileRequest;
import com.commonlibrary.dto.CustomPatientDto;
import com.commonlibrary.dto.PatientProfileDto;
import com.commonlibrary.entity.SupervisorAssignmentStatus;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.dto.PatientDto;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.dto.AssignPatientByEmailRequest;
import com.supervisorservice.dto.PatientAssignmentDto;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorPatientAssignment;
import com.supervisorservice.exception.*;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing patient assignments to supervisors
 *
 * Supports THREE assignment options:
 * OPTION 1: Create new patient profile and auto-assign to supervisor
 * OPTION 2: Assign existing patient by email (with verification)
 * OPTION 3: Assign existing patient by ID (original method)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientManagementService {

    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final SupervisorValidationService validationService;
    private final SupervisorKafkaProducer kafkaProducer;
    private final PatientServiceClient patientServiceClient;
    private final MedicalSupervisorRepository medicalSupervisorRepository;
    private final SupervisorPatientAssignmentRepository supervisorPatientAssignmentRepository;

    /**
     * OPTION 1: Create new patient profile and assign to supervisor
     *
     * This method:
     * 1. Validates supervisor is active and has capacity
     * 2. Calls patient-service to create user account and patient profile
     * 3. Auto-assigns the new patient to the supervisor (if requested)
     * 4. Publishes Kafka event for patient assignment
     *
     * @param supervisorUserId The user ID of the supervisor
     * @param request Complete patient profile information
     * @return PatientAssignmentDto with assignment details
     * @throws PatientLimitExceededException if supervisor has reached patient limit
     * @throws RuntimeException if patient creation fails
     */
    @Transactional
    public PatientAssignmentDto createAndAssignPatient(Long supervisorUserId, CreatePatientProfileRequest request) {
        log.info("Creating and assigning new patient for supervisor userId: {}", supervisorUserId);

        // Validate supervisor is active
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(supervisorUserId);

        // Check patient limit
        Long currentPatientCount = assignmentRepository.countActiveAssignmentsBySupervisor(supervisor.getId());
        if (currentPatientCount >= supervisor.getMaxPatientsLimit()) {
            throw new PatientLimitExceededException(
                    String.format("Maximum patient limit reached (%d/%d). Cannot assign more patients.",
                            currentPatientCount, supervisor.getMaxPatientsLimit()));
        }

        // Create patient profile via patient-service
        PatientProfileDto createdPatient;
        try {
            // Call patient-service to create patient account
            // This will:
            // 1. Create user account in auth-service
            // 2. Create patient profile in patient-service
            // 3. Send welcome email with temporary password
            // 4. Return complete patient information
            createdPatient = patientServiceClient.createPatientBySupervisor(request, supervisor.getId()).getBody().getData();

            log.info("Patient created successfully - patientId: {}, email: {}",
                    createdPatient.getId(), createdPatient.getEmail());

        } catch (Exception e) {
            log.error("Failed to create patient profile: {}", e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("Failed to create patient account. Please check the information and try again.");
        }

        // Auto-assign to supervisor if requested
        if (request.getAutoAssignToSupervisor()) {
            SupervisorPatientAssignment assignment = SupervisorPatientAssignment.builder()
                    .supervisor(supervisor)
                    .patientId(createdPatient.getId())
                    .patientUserId(createdPatient.getUserId())
                    .assignedAt(LocalDateTime.now())
                    .assignmentNotes(request.getNotes())
                    .patientEmail(createdPatient.getEmail())
                    .patientName(createdPatient.getFullName())
                    .patientPhone(createdPatient.getPhoneNumber())
                    //.isActive(true)
                    .assignmentStatus(SupervisorAssignmentStatus.ACTIVE)
                    .build();

            assignment = assignmentRepository.save(assignment);

            // Publish Kafka event for patient assignment
            // This will be consumed by patient-service to update patient.managedBySupervisor flag
            kafkaProducer.sendPatientAssignedEvent(assignment);

            log.info("Patient auto-assigned - assignmentId: {}", assignment.getId());

            return convertToDto(assignment, createdPatient);
        }

        // Return patient info without assignment
        return PatientAssignmentDto.builder()
                .patientId(createdPatient.getId())
                .patientName(createdPatient.getFullName())
                .patientEmail(createdPatient.getEmail())
                .assignedAt(LocalDateTime.now())
                //.isActive(false)
                .build();
    }

    /**
     * OPTION 2: Assign existing patient by email
     * This method:
     * 1. Validates supervisor is active and has capacity
     * 2. Searches for patient by email in patient-service
     * 3. Optionally verifies patient name for security
     * 4. Creates assignment record
     * 5. Publishes Kafka event for patient assignment
     * @param supervisorUserId The user ID of the supervisor
     * @param request Patient email and optional verification info
     * @return PatientAssignmentDto with assignment details
     * @throws ResourceNotFoundException if patient not found
     * @throws ValidationException if patient name doesn't match
     * @throws DuplicateResourceException if patient already assigned
     */
    @Transactional
    public PatientAssignmentDto assignExistingPatientByEmail(Long supervisorUserId, AssignPatientByEmailRequest request) {
        log.info("Assigning existing patient by email for supervisor userId: {}", supervisorUserId);

        // Validate supervisor is active
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(supervisorUserId);

        // Check patient limit
        Long currentPatientCount = assignmentRepository.countActiveAssignmentsBySupervisor(supervisor.getId());
        if (currentPatientCount >= supervisor.getMaxPatientsLimit()) {
            throw new PatientLimitExceededException(
                    String.format("Maximum patient limit reached (%d/%d). Cannot assign more patients.",
                            currentPatientCount, supervisor.getMaxPatientsLimit()));
        }

        // Get patient by email from patient-service
        PatientProfileDto patient;

        try {
            patient = patientServiceClient.getPatientByEmail(request.getPatientEmail()).getBody().getData();

            if (patient == null) {
                throw new ResourceNotFoundException(
                        "No patient found with email: " + request.getPatientEmail());
            }

            // Verify patient name if provided (for security)
            if (request.getPatientFullName() != null &&
                    !patient.getFullName().equalsIgnoreCase(request.getPatientFullName().trim())) {
                throw new ValidationException(
                        "Patient name verification failed. Expected: " + request.getPatientFullName() +
                                ", Found: " + patient.getFullName());
            }

            log.info("Found patient - patientId: {}, name: {}", patient.getId(), patient.getFullName());

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch patient by email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to verify patient. Please check the email and try again.");
        }

        // Check if patient is already assigned to this supervisor
        boolean alreadyAssigned = assignmentRepository.existsByAssignmentKey(supervisor.getId(), patient.getId());
        if (alreadyAssigned) {
            throw new DuplicateResourceException(
                    "Patient is already assigned to this supervisor. Please check your assigned patients list.");
        }

        // Create assignment
        SupervisorPatientAssignment assignment = SupervisorPatientAssignment.builder()
                .supervisor(supervisor)
                .patientId(patient.getId())
                .patientUserId(patient.getUserId())
                .assignedAt(LocalDateTime.now())
                .assignmentNotes(request.getAssignmentNotes())
                //.isActive(true)
                .assignmentStatus(SupervisorAssignmentStatus.ACTIVE)
                .build();

        assignment = assignmentRepository.save(assignment);

        // Publish Kafka event for patient assignment
        // This will be consumed by patient-service to update patient.managedBySupervisor flag
        kafkaProducer.sendPatientAssignedEvent(assignment);

        log.info("Existing patient assigned successfully - assignmentId: {}", assignment.getId());

        return convertToDto(assignment, patient);
    }

    /**
     * OPTION 3: Assign existing patient by ID (original method)
     *
     * This is the original assignment method kept for backward compatibility.
     * Use this when you already have the patient ID.
     *
     * @param userId The user ID of the supervisor
     * @param patientId The ID of the patient to assign
     * @param notes Optional notes about the assignment
     * @return PatientAssignmentDto with assignment details
     */
    @Transactional
    public PatientAssignmentDto assignPatient(Long userId, Long patientId, String notes) {
        log.info("Assigning patient {} to supervisor userId: {}", patientId, userId);

        // Validate supervisor is active
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        // Validate supervisor can add more patients
        validationService.validateCanAddPatient(supervisor);

        // Validate patient is not already assigned
        validationService.validatePatientNotAssigned(patientId);


        //Get patient's info
        String patientFullName = "Not assigned";
        String patientEmail = "Not assigned";
        String patientPhone = "Not assigned";
        Map<String, Object> patientInfo = patientServiceClient.getPatientBasicInfo(patientId).getData();
        if (patientInfo == null || !patientInfo.containsKey("fullName") ||
                !patientInfo.containsKey("email") || !patientInfo.containsKey("phoneNumber")) {
            log.warn("Failed to get patient's info");
        }
        else{
            patientFullName = patientInfo.get("fullName").toString();
            patientEmail = patientInfo.get("email").toString();
            patientPhone = patientInfo.get("phoneNumber").toString();
        }

        // Create assignment
        SupervisorPatientAssignment assignment = SupervisorPatientAssignment.builder()
                .supervisor(supervisor)
                .patientId(patientId)
                .assignedAt(LocalDateTime.now())
                .assignmentNotes(notes)
                .patientPhone(patientPhone)
                .patientName(patientFullName)
                .patientEmail(patientEmail)
                //.isActive(true)
                .assignmentStatus(SupervisorAssignmentStatus.ACTIVE)
                .build();

        assignment = assignmentRepository.save(assignment);

        // Publish Kafka event
        kafkaProducer.sendPatientAssignedEvent(assignment);

        log.info("Patient assigned successfully - assignmentId: {}", assignment.getId());

        return convertToDto(assignment, null);
    }

    /**
     * Remove patient assignment
     *
     * This marks the assignment as inactive and publishes an event.
     * Patient-service will update the patient.managedBySupervisor flag.
     *
     * @param userId The user ID of the supervisor
     * @param patientId The ID of the patient to remove
     * @param reason Reason for removal
     */
    @Transactional
    public void removePatientAssignment(Long userId, Long patientId, String reason) {
        log.info("Removing patient {} assignment for supervisor userId: {}", patientId, userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        SupervisorPatientAssignment assignment = assignmentRepository
                .findActiveAssignment(supervisor.getId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient assignment not found"));

        SupervisorPatientAssignment tempAssignment = new SupervisorPatientAssignment();
        tempAssignment = assignment;
        assignment.remove(reason);

        assignmentRepository.save(assignment);

        // Publish Kafka event for patient removal
        // This will be consumed by patient-service to update patient.managedBySupervisor = false
        kafkaProducer.sendPatientRemovedEvent(tempAssignment, reason);

        log.info("Patient assignment removed successfully");
    }

    /**
     * Get all assigned patients for supervisor
     *
     * @param userId The user ID of the supervisor
     * @return List of active patient assignments
     */
    @Transactional(readOnly = true)
    public List<PatientAssignmentDto> getAssignedPatients(Long userId) {
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        List<SupervisorPatientAssignment> assignments =
                assignmentRepository.findActiveAssignmentsBySupervisor(supervisor);

        return assignments.stream()
                .map(assignment -> convertToDto(assignment, null))
                .collect(Collectors.toList());
    }

    /**
     * Get specific patient assignment
     *
     * @param userId The user ID of the supervisor
     * @param patientId The ID of the patient
     * @return PatientAssignmentDto with assignment details
     */
    @Transactional(readOnly = true)
    public PatientAssignmentDto getPatientAssignment(Long userId, Long patientId) {
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        SupervisorPatientAssignment assignment = assignmentRepository
                .findActiveAssignment(supervisor.getId(), patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient assignment not found"));

        return convertToDto(assignment, null);
    }

    /**
     * Get patient IDs for supervisor
     *
     * @param userId The user ID of the supervisor
     * @return List of patient IDs
     */
    @Transactional(readOnly = true)
    public List<Long> getAssignedPatientIds(Long userId) {
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        return assignmentRepository.findPatientIdsBySupervisor(supervisor.getId());
    }

    /**
     * Get patient UserIDs for supervisor
     *
     * @param userId The user ID of the supervisor
     * @return List of patient UserIDs
     */
    @Transactional(readOnly = true)
    public List<Long> getAssignedPatientUserIds(Long userId) {
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        return assignmentRepository.findPatientUserIdsBySupervisor(supervisor.getId());
    }


    public CustomPatientDto getCustomPatientInfo(Long caseId, Long userId, Long patientId) {
        try {
            // Validate supervisor is active
            MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

            //check if supervisor assignment is active
            validationService.validatePatientAccess(supervisor.getId(), patientId);

            // Call patient service to get case details
            CustomPatientDto patientDto = patientServiceClient.getCustomPatientInfo(caseId, supervisor.getId()).getBody().getData();
            return patientDto;
        } catch (Exception e) {
            log.error("Error getting patient custom info for case {}: {}", caseId, e.getMessage());
            e.printStackTrace();
            throw new BusinessException("Error getting patient custom info", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convert assignment entity to DTO
     *
     * @param assignment The assignment entity
     * @param patient Optional patient details from patient-service
     * @return PatientAssignmentDto
     */
    private PatientAssignmentDto convertToDto(SupervisorPatientAssignment assignment, PatientProfileDto patient) {
        PatientAssignmentDto dto = PatientAssignmentDto.builder()
                .id(assignment.getId())
                .supervisorId(assignment.getSupervisor().getId())
                .patientId(assignment.getPatientId())
                .assignedAt(assignment.getAssignedAt())
                .assignmentNotes(assignment.getAssignmentNotes())
                .patientEmail(assignment.getPatientEmail())
                .patientName(assignment.getPatientName())
                .patientPhoneNumber(assignment.getPatientPhone())
                .assignmentStatus(assignment.getAssignmentStatus() != null ? assignment.getAssignmentStatus() :
                        SupervisorAssignmentStatus.ACTIVE)
                //.isActive(assignment.getIsActive())
                .build();

        // Add patient details if provided
        if (patient != null) {
            dto.setPatientName(patient.getFullName());
            dto.setPatientEmail(patient.getEmail());
            dto.setPatientName(patient.getPhoneNumber());
        }

        return dto;
    }
}