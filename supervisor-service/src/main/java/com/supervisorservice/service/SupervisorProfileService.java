package com.supervisorservice.service;

import com.commonlibrary.entity.SupervisorVerificationStatus;
import com.supervisorservice.dto.CreateSupervisorProfileRequest;
import com.commonlibrary.dto.SupervisorProfileDto;
import com.supervisorservice.dto.UpdateSupervisorProfileRequest;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorSettings;
import com.supervisorservice.exception.DuplicateResourceException;
import com.supervisorservice.exception.ResourceNotFoundException;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for managing supervisor profiles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupervisorProfileService {
    
    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorSettingsRepository settingsRepository;
    private final SupervisorValidationService validationService;
    private final SupervisorKafkaProducer kafkaProducer;
    
    @Value("${supervisor.max-patients-default:3}")
    private Integer defaultMaxPatients;
    
    @Value("${supervisor.max-active-cases-per-patient:2}")
    private Integer defaultMaxCasesPerPatient;
    
    @Value("${app.file.upload.dir:./uploads/supervisor}")
    private String uploadDir;
    
    /**
     * Create supervisor profile
     */
    @Transactional
    public SupervisorProfileDto createProfile(Long userId, CreateSupervisorProfileRequest request) {
        log.info("Creating supervisor profile for userId: {}", userId);
        
        // Check if supervisor already exists
        if (supervisorRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Supervisor profile already exists for this user");
        }
        
        // Validate email uniqueness
        validationService.validateEmailUnique(request.getEmail(), null);
        
        // Create supervisor entity
        MedicalSupervisor supervisor = MedicalSupervisor.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .organizationName(request.getOrganizationName())
                .organizationType(request.getOrganizationType())
                .licenseNumber(request.getLicenseNumber())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .verificationStatus(SupervisorVerificationStatus.PENDING)
                .maxPatientsLimit(defaultMaxPatients)
                .maxActiveCasesPerPatient(defaultMaxCasesPerPatient)
                .isAvailable(true)
                .isDeleted(false)
                .build();
        
        supervisor = supervisorRepository.save(supervisor);
        log.info("Supervisor profile created with id: {}", supervisor.getId());
        
        // Create default settings
        createDefaultSettings(supervisor);
        
        // Publish event
        kafkaProducer.sendSupervisorRegisteredEvent(supervisor);
        
        return mapToDto(supervisor);
    }
    
    /**
     * Get supervisor profile by user ID
     */
    @Transactional(readOnly = true)
    public SupervisorProfileDto getProfile(Long userId) {
        log.debug("Getting supervisor profile for userId: {}", userId);
        
        MedicalSupervisor supervisor = supervisorRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor profile not found"));
        
        return mapToDto(supervisor);
    }
    
    /**
     * Update supervisor profile
     */
    @Transactional
    public SupervisorProfileDto updateProfile(Long userId, UpdateSupervisorProfileRequest request) {
        log.info("Updating supervisor profile for userId: {}", userId);
        
        MedicalSupervisor supervisor = supervisorRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor profile not found"));
        
        // Validate email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(supervisor.getEmail())) {
            validationService.validateEmailUnique(request.getEmail(), supervisor.getId());
            supervisor.setEmail(request.getEmail());
        }
        
        // Update fields if provided
        if (request.getFullName() != null) {
            supervisor.setFullName(request.getFullName());
        }
        if (request.getOrganizationName() != null) {
            supervisor.setOrganizationName(request.getOrganizationName());
        }
        if (request.getOrganizationType() != null) {
            supervisor.setOrganizationType(request.getOrganizationType());
        }
        if (request.getLicenseNumber() != null) {
            supervisor.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getPhoneNumber() != null) {
            supervisor.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            supervisor.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            supervisor.setCity(request.getCity());
        }
        if (request.getCountry() != null) {
            supervisor.setCountry(request.getCountry());
        }
        
        supervisor = supervisorRepository.save(supervisor);
        log.info("Supervisor profile updated: {}", supervisor.getId());
        
        return mapToDto(supervisor);
    }
    
    /**
     * Upload license document
     */
    @Transactional
    public String uploadLicenseDocument(Long userId, MultipartFile file) throws IOException {
        log.info("Uploading license document for userId: {}", userId);
        
        MedicalSupervisor supervisor = supervisorRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor profile not found"));
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = String.format("license_%d_%s%s", 
                supervisor.getId(), 
                UUID.randomUUID().toString(), 
                extension);
        
        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Update supervisor with file path
        supervisor.setLicenseDocumentPath(filePath.toString());
        supervisorRepository.save(supervisor);
        
        log.info("License document uploaded successfully: {}", filename);
        return filePath.toString();
    }
    
    /**
     * Delete supervisor profile (soft delete)
     */
    @Transactional
    public void deleteProfile(Long userId) {
        log.info("Deleting supervisor profile for userId: {}", userId);
        
        MedicalSupervisor supervisor = supervisorRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor profile not found"));
        
        supervisor.setIsDeleted(true);
        supervisor.setIsAvailable(false);
        supervisorRepository.save(supervisor);
        
        log.info("Supervisor profile soft deleted: {}", supervisor.getId());
    }
    
    /**
     * Create default settings for new supervisor
     */
    private void createDefaultSettings(MedicalSupervisor supervisor) {
        SupervisorSettings settings = SupervisorSettings.builder()
                .supervisor(supervisor)
                .emailNotifications(true)
                .smsNotifications(false)
                .pushNotifications(true)
                .newCaseAssignmentNotification(true)
                .appointmentRemindersNotification(true)
                .caseStatusUpdateNotification(true)
                .couponIssuedNotification(true)
                .couponExpiringNotification(true)
                .preferredLanguage("EN")
                .timezone("UTC")
                .theme("light")
                .build();
        
        settingsRepository.save(settings);
        log.debug("Default settings created for supervisor: {}", supervisor.getId());
    }
    
    /**
     * Map entity to DTO
     */
    private SupervisorProfileDto mapToDto(MedicalSupervisor supervisor) {
        return SupervisorProfileDto.builder()
                .id(supervisor.getId())
                .userId(supervisor.getUserId())
                .fullName(supervisor.getFullName())
                .organizationName(supervisor.getOrganizationName())
                .organizationType(supervisor.getOrganizationType())
                .licenseNumber(supervisor.getLicenseNumber())
                .licenseDocumentPath(supervisor.getLicenseDocumentPath())
                .phoneNumber(supervisor.getPhoneNumber())
                .email(supervisor.getEmail())
                .address(supervisor.getAddress())
                .city(supervisor.getCity())
                .country(supervisor.getCountry())
                .verificationStatus(supervisor.getVerificationStatus())
                .verificationNotes(supervisor.getVerificationNotes())
                .verifiedAt(supervisor.getVerifiedAt())
                .verifiedBy(supervisor.getVerifiedBy())
                .rejectionReason(supervisor.getRejectionReason())
                .maxPatientsLimit(supervisor.getMaxPatientsLimit())
                .maxActiveCasesPerPatient(supervisor.getMaxActiveCasesPerPatient())
                .isAvailable(supervisor.getIsAvailable())
                .activePatientCount(supervisor.getActivePatientCount())
                .availableCouponCount(supervisor.getAvailableCouponCount())
                .createdAt(supervisor.getCreatedAt())
                .updatedAt(supervisor.getUpdatedAt())
                .build();
    }
}
