package com.supervisorservice.service;

import com.commonlibrary.entity.SupervisorAssignmentStatus;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorCoupon;
import com.supervisorservice.entity.SupervisorPatientAssignment;
import com.supervisorservice.exception.*;
import com.supervisorservice.exception.ResourceNotFoundException;
import com.supervisorservice.exception.SupervisorNotVerifiedException;
import com.supervisorservice.exception.PatientLimitExceededException;
import com.supervisorservice.exception.CaseLimitExceededException;
import com.supervisorservice.exception.InvalidCouponException;
import com.supervisorservice.exception.CouponExpiredException;
import com.supervisorservice.exception.CouponAlreadyUsedException;
import com.supervisorservice.exception.UnauthorizedPatientAccessException;
import com.supervisorservice.exception.DuplicateResourceException;
import com.supervisorservice.exception.ValidationException;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorCouponRepository;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for validating supervisor business rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupervisorValidationService {
    
    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final SupervisorCouponRepository couponRepository;
    
    /**
     * Validate and get supervisor by user ID
     * Throws exception if not found or not active
     */
    public MedicalSupervisor validateSupervisorActive(Long userId) {
        log.debug("Validating supervisor with userId: {}", userId);
        
        MedicalSupervisor supervisor = supervisorRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "userId: " + userId));
        
        if (!supervisor.isActiveAndVerified()) {
            if (supervisor.getVerificationStatus() == SupervisorVerificationStatus.PENDING) {
                throw new SupervisorNotVerifiedException(
                        "Your supervisor application is pending verification. Please wait for admin approval.");
            } else if (supervisor.getVerificationStatus() == SupervisorVerificationStatus.REJECTED) {
                throw new SupervisorNotVerifiedException(
                        "Your supervisor application was rejected. Reason: " + supervisor.getRejectionReason());
            } else if (supervisor.getVerificationStatus() == SupervisorVerificationStatus.SUSPENDED) {
                throw new SupervisorNotVerifiedException(
                        "Your supervisor account has been suspended. Please contact support.");
            } else if (!supervisor.getIsAvailable()) {
                throw new SupervisorNotVerifiedException(
                        "Your supervisor account is currently unavailable.");
            }
        }
        
        log.debug("Supervisor validated successfully: {}", supervisor.getId());
        return supervisor;
    }
    
    /**
     * Validate supervisor has access to patient
     */
    public void validatePatientAccess(Long supervisorId, Long patientId) {
        log.debug("Validating patient access - supervisorId: {}, patientId: {}", supervisorId, patientId);
        
        boolean hasAccess = assignmentRepository.existsActiveAssignment(supervisorId, patientId);
        
        if (!hasAccess) {
            throw new UnauthorizedPatientAccessException(
                    "You do not have access to this patient. The patient must be assigned to you first.");
        }
        
        log.debug("Patient access validated successfully");
    }
    
    /**
     * Validate supervisor can add more patients
     */
    public void validateCanAddPatient(MedicalSupervisor supervisor) {
        log.debug("Validating if supervisor can add more patients: {}", supervisor.getId());
        
        Long activePatientCount = assignmentRepository.countActiveAssignmentsBySupervisor(supervisor.getId());
        
        if (activePatientCount >= supervisor.getMaxPatientsLimit()) {
            throw new PatientLimitExceededException(
                    String.format("You have reached your maximum patient limit (%d/%d). " +
                            "Please contact support to increase your limit.",
                            activePatientCount, supervisor.getMaxPatientsLimit()));
        }
        
        log.debug("Supervisor can add more patients: {}/{}", activePatientCount, supervisor.getMaxPatientsLimit());
    }
    
    /**
     * Validate supervisor can submit case for patient
     * Checks active case limit per patient
     */
    public void validateCanSubmitCase(MedicalSupervisor supervisor, Long patientId) {
        log.debug("Validating if supervisor can submit case for patient: {}", patientId);
        
        // First validate patient access
        validatePatientAccess(supervisor.getId(), patientId);
        
        // Note: Actual case count would be checked via Feign client to patient-service
        // This is a placeholder for the validation logic
        // In the actual implementation, we would call patient-service to get active case count
        
        log.debug("Case submission validation passed for patient: {}", patientId);
    }
    
    /**
     * Validate coupon is usable
     */
    public SupervisorCoupon validateCouponUsable(String couponCode, Long patientId) {
        log.debug("Validating coupon: {} for patient: {}", couponCode, patientId);
        
        SupervisorCoupon coupon = couponRepository.findAvailableCoupon(couponCode, LocalDateTime.now())
                .orElseThrow(() -> {
                    // Check if coupon exists but is not available
                    return couponRepository.findByCouponCodeAndIsDeletedFalse(couponCode)
                            .map(c -> {
                                if (c.getStatus() == com.commonlibrary.entity.CouponStatus.USED) {
                                    return new CouponAlreadyUsedException(
                                            "This coupon has already been used on " + c.getUsedAt());
                                } else if (c.getStatus() == com.commonlibrary.entity.CouponStatus.EXPIRED ||
                                        c.getExpiresAt().isBefore(LocalDateTime.now())) {
                                    return new CouponExpiredException(
                                            "This coupon expired on " + c.getExpiresAt());
                                } else if (c.getStatus() == com.commonlibrary.entity.CouponStatus.CANCELLED) {
                                    return new InvalidCouponException(
                                            "This coupon has been cancelled. Reason: " + c.getCancellationReason());
                                } else {
                                    return new InvalidCouponException("This coupon is not available for use.");
                                }
                            })
                            .orElse(new InvalidCouponException("Invalid coupon code: " + couponCode));
                });
        
        // Validate coupon belongs to the patient
        if (!coupon.getPatientId().equals(patientId)) {
            throw new InvalidCouponException(
                    "This coupon is assigned to a different patient and cannot be used.");
        }
        
        log.debug("Coupon validated successfully: {}", couponCode);
        return coupon;
    }
    
    /**
     * Validate patient is not already assigned to another supervisor
     */
    public void validatePatientNotAssigned(Long patientId) {
        log.debug("Validating patient is not already assigned: {}", patientId);
        
        boolean hasActiveSupervisor = assignmentRepository.patientHasActiveSupervisor(patientId);
        
        if (hasActiveSupervisor) {
            throw new DuplicateResourceException(
                    "This patient is already assigned to another supervisor. " +
                    "A patient can only have one active supervisor at a time.");
        }
        
        log.debug("Patient is not assigned to any supervisor");
    }
    
    /**
     * Validate supervisor exists by ID
     */
    public MedicalSupervisor validateSupervisorExists(Long supervisorId) {
        log.debug("Validating supervisor exists: {}", supervisorId);
        
        return supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "id: " + supervisorId));
    }
    
    /**
     * Validate email is unique
     */
    public void validateEmailUnique(String email, Long excludeSupervisorId) {
        log.debug("Validating email uniqueness: {}", email);
        
        boolean exists = excludeSupervisorId != null
                ? supervisorRepository.existsByEmailAndIdNot(email, excludeSupervisorId)
                : supervisorRepository.existsByEmail(email);
        
        if (exists) {
            throw new DuplicateResourceException(
                    "A supervisor with this email address already exists: " + email);
        }
        
        log.debug("Email is unique: {}", email);
    }


    /**
     * Validate that supervisor is verified and active
     * @param supervisor MedicalSupervisor entity
     */
    public void validateSupervisorVerified(MedicalSupervisor supervisor) {
        if (supervisor.getVerificationStatus() != SupervisorVerificationStatus.VERIFIED) {
            throw new BusinessException(
                    "Supervisor must be verified to perform this action. Current status: "
                            + supervisor.getVerificationStatus(),
                    HttpStatus.FORBIDDEN);
        }

        if (supervisor.getIsDeleted()) {
            throw new BusinessException("Supervisor account has been deleted", HttpStatus.FORBIDDEN);
        }

        if (!supervisor.getIsAvailable()) {
            throw new BusinessException("Supervisor account is currently unavailable", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Validate that a patient is assigned to the supervisor
     * @param supervisorId Supervisor ID
     * @param patientId Patient ID
     */
    public void validatePatientAssignment(Long supervisorId, Long patientId) {
        boolean isAssigned = assignmentRepository.existsBySupervisorIdAndPatientIdAndAssignmentStatus(
                supervisorId,
                patientId,
                SupervisorAssignmentStatus.ACTIVE);

        if (!isAssigned) {
            throw new BusinessException(
                    "Patient " + patientId + " is not assigned to this supervisor",
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get assignment if exists and is active
     * @param supervisorId Supervisor ID
     * @param patientId Patient ID
     * @return Optional assignment
     */
    public Optional<SupervisorPatientAssignment> getActiveAssignment(Long supervisorId, Long patientId) {
        return assignmentRepository.findBySupervisorIdAndPatientIdAndAssignmentStatus(
                supervisorId,
                patientId,
                SupervisorAssignmentStatus.ACTIVE);
    }

    /**
     * Validate supervisor can manage more patients
     * @param supervisor MedicalSupervisor entity
     */
    public void validatePatientLimit(MedicalSupervisor supervisor) {
        long currentPatients = assignmentRepository.countBySupervisorIdAndAssignmentStatus(
                supervisor.getId(),
                SupervisorAssignmentStatus.ACTIVE);

        Integer maxLimit = supervisor.getMaxPatientsLimit();
        if (maxLimit != null && currentPatients >= maxLimit) {
            throw new BusinessException(
                    "Maximum patient limit reached. Current: " + currentPatients + ", Max: " + maxLimit,
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Validate supervisor exists by user ID
     * @param userId User ID
     * @return MedicalSupervisor entity
     */
    public MedicalSupervisor getSupervisorByUserId(Long userId) {
        return supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Check if supervisor profile exists
     * @param userId User ID
     * @return true if profile exists
     */
    public boolean supervisorProfileExists(Long userId) {
        return supervisorRepository.existsByUserId(userId);
    }
}
