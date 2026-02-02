package com.supervisorservice.service;

import com.commonlibrary.constants.CouponErrorCodes;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PatientProfileDto;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.SupervisorAssignmentStatus;
import com.commonlibrary.entity.SupervisorCouponStatus;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import com.supervisorservice.feign.AdminServiceClient;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.kafka.SupervisorCouponEventProducer;
import com.supervisorservice.mapper.SupervisorCouponAllocationMapper;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorCouponAllocationRepository;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing supervisor coupon allocations and redemptions.
 * This service works with local allocation data and syncs with admin-service for validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupervisorCouponService {

    private final SupervisorCouponAllocationRepository allocationRepository;
    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final AdminServiceClient adminCouponClient;
    private final PatientServiceClient patientServiceClient;
    private final SupervisorCouponAllocationMapper allocationMapper;
    private final SupervisorCouponEventProducer eventProducer;

    // ==================== Coupon Assignment to Patient ====================

    /**
     * Assign an unassigned coupon to a specific patient.
     * This is a local operation - supervisor allocates their coupon to a patient.
     */
    public SupervisorCouponAllocationDto assignCouponToPatient(
            Long supervisorUserId,
            Long couponAllocationId,
            AssignCouponToPatientRequest request) {
        
        log.info("Supervisor {} assigning coupon {} to patient {}", 
                supervisorUserId, couponAllocationId, request.getPatientId());

        // Get supervisor
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);

        // Validate patient is assigned to supervisor
        validatePatientAssignment(supervisor.getId(), request.getPatientId());

        // Get allocation
        SupervisorCouponAllocation allocation = allocationRepository.findById(couponAllocationId)
                .orElseThrow(() -> new BusinessException("Coupon allocation not found", HttpStatus.NOT_FOUND));

        // Validate allocation belongs to supervisor
        if (!allocation.getSupervisorId().equals(supervisor.getId())) {
            throw new BusinessException("Coupon does not belong to this supervisor", HttpStatus.FORBIDDEN);
        }

        // Validate coupon is available for assignment
        if (!allocation.isAvailableForAssignment()) {
            throw new BusinessException(
                    "Coupon is not available for assignment. Status: " + allocation.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        // Assign to patient
        allocation.setAssignedPatientId(request.getPatientId());
        allocation.setAssignedAt(LocalDateTime.now());
        allocation.setAssignmentNotes(request.getNotes());
        allocation.setStatus(SupervisorCouponStatus.ASSIGNED);

        SupervisorCouponAllocation saved = allocationRepository.save(allocation);
        log.info("Coupon {} assigned to patient {}", allocation.getCouponCode(), request.getPatientId());

        // Send event
        eventProducer.sendCouponAssignedEvent(saved, supervisor.getId());

        return enrichWithPatientName(allocationMapper.toDto(saved));
    }

    /**
     * Unassign a coupon from a patient (make it available again).
     * Only allowed if coupon hasn't been used.
     */
    public SupervisorCouponAllocationDto unassignCouponFromPatient(
            Long supervisorUserId,
            Long couponAllocationId) {
        
        log.info("Supervisor {} unassigning coupon {}", supervisorUserId, couponAllocationId);

        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);

        SupervisorCouponAllocation allocation = allocationRepository.findById(couponAllocationId)
                .orElseThrow(() -> new BusinessException("Coupon allocation not found", HttpStatus.NOT_FOUND));

        // Validate ownership
        if (!allocation.getSupervisorId().equals(supervisor.getId())) {
            throw new BusinessException("Coupon does not belong to this supervisor", HttpStatus.FORBIDDEN);
        }

        // Validate coupon can be unassigned
        if (allocation.getStatus() == SupervisorCouponStatus.USED) {
            throw new BusinessException("Cannot unassign a used coupon", HttpStatus.BAD_REQUEST);
        }

        if (allocation.getStatus() != SupervisorCouponStatus.ASSIGNED) {
            throw new BusinessException("Coupon is not assigned to a patient", HttpStatus.BAD_REQUEST);
        }

        Long previousPatientId = allocation.getAssignedPatientId();

        // Unassign
        allocation.setAssignedPatientId(null);
        allocation.setAssignedAt(null);
        allocation.setAssignmentNotes(null);
        allocation.setStatus(SupervisorCouponStatus.AVAILABLE);

        SupervisorCouponAllocation saved = allocationRepository.save(allocation);
        log.info("Coupon {} unassigned from patient {}", allocation.getCouponCode(), previousPatientId);

        // Send event
        eventProducer.sendCouponUnassignedEvent(saved, supervisor.getId(), previousPatientId);

        return allocationMapper.toDto(saved);
    }

    // ==================== Coupon Validation ====================

    /**
     * Validate a coupon for use by a patient.
     * Calls admin-service for authoritative validation.
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(
            Long supervisorUserId,
            String couponCode,
            Long patientId,
            Long caseId,
            BigDecimal requestedAmount) {
        
        log.info("Validating coupon {} for patient {} case {}", couponCode, patientId, caseId);

        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);

        // Validate patient is assigned to supervisor
        validatePatientAssignment(supervisor.getId(), patientId);

        // First check local allocation
        SupervisorCouponAllocation localAllocation = allocationRepository
                .findByCouponCodeAndSupervisorId(couponCode.toUpperCase(), supervisor.getId())
                .orElse(null);

        if (localAllocation == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .message("Coupon not found in your allocations")
                    .errorCode(CouponErrorCodes.COUPON_NOT_FOUND)
                    .build();
        }

        // Check local status
        if (localAllocation.getStatus() == SupervisorCouponStatus.USED) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .message("Coupon has already been used")
                    .errorCode(CouponErrorCodes.COUPON_ALREADY_USED)
                    .build();
        }

        if (localAllocation.isExpired()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .message("Coupon has expired")
                    .errorCode(CouponErrorCodes.COUPON_EXPIRED)
                    .build();
        }

        // Check patient assignment
        if (localAllocation.getAssignedPatientId() == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .message("Coupon is not assigned to any patient")
                    .errorCode(CouponErrorCodes.COUPON_NOT_DISTRIBUTED)
                    .build();
        }

        if (!localAllocation.getAssignedPatientId().equals(patientId)) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .message("Coupon is assigned to a different patient")
                    .errorCode(CouponErrorCodes.COUPON_PATIENT_MISMATCH)
                    .build();
        }

        // Call admin-service for authoritative validation
        try {
            CouponValidationRequest adminRequest = CouponValidationRequest.builder()
                    .couponCode(couponCode)
                    .beneficiaryType(BeneficiaryType.MEDICAL_SUPERVISOR)
                    .beneficiaryId(supervisor.getId())
                    .patientId(patientId)
                    .caseId(caseId)
                    .requestedAmount(requestedAmount)
                    .build();

            ResponseEntity<ApiResponse<CouponValidationResponse>> response = 
                    adminCouponClient.validateCoupon(adminRequest);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.warn("Failed to validate with admin-service, using local validation: {}", e.getMessage());
        }

        // Fallback to local validation if admin-service unavailable
        BigDecimal discountAmount = localAllocation.calculateDiscount(requestedAmount);
        BigDecimal remainingAmount = requestedAmount.subtract(discountAmount);

        return CouponValidationResponse.builder()
                .valid(true)
                .couponId(localAllocation.getAdminCouponId())
                .couponCode(localAllocation.getCouponCode())
                .discountType(localAllocation.getDiscountType())
                .discountValue(localAllocation.getDiscountValue())
                .maxDiscountAmount(localAllocation.getMaxDiscountAmount())
                .discountAmount(discountAmount)
                .remainingAmount(remainingAmount)
                .originalAmount(requestedAmount)
                .currency(localAllocation.getCurrency())
                .expiresAt(localAllocation.getExpiresAt())
                .patientId(patientId)
                .beneficiaryId(supervisor.getId())
                .message("Coupon is valid (local validation)")
                .build();
    }

    // ==================== Coupon Retrieval ====================

    /**
     * Get all coupon allocations for supervisor
     */
    @Transactional(readOnly = true)
    public List<SupervisorCouponAllocationDto> getAllCouponsForSupervisor(Long supervisorUserId) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        List<SupervisorCouponAllocation> allocations = allocationRepository
                .findBySupervisorIdOrderByCreatedAtDesc(supervisor.getId());
        return enrichWithPatientNames(allocationMapper.toDtoList(allocations));
    }

    /**
     * Get unassigned (available) coupons for supervisor
     */
    @Transactional(readOnly = true)
    public List<SupervisorCouponAllocationDto> getUnassignedCoupons(Long supervisorUserId) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        List<SupervisorCouponAllocation> allocations = allocationRepository
                .findUnassignedCouponsForSupervisor(supervisor.getId(), LocalDateTime.now());
        return allocationMapper.toDtoList(allocations);
    }

    /**
     * Get coupons available for a specific patient
     */
    @Transactional(readOnly = true)
    public List<SupervisorCouponAllocationDto> getAvailableCouponsForPatient(
            Long supervisorUserId, Long patientId) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        
        // Validate patient assignment
        validatePatientAssignment(supervisor.getId(), patientId);

        List<SupervisorCouponAllocation> allocations = allocationRepository
                .findAvailableCouponsForPatient(supervisor.getId(), patientId, LocalDateTime.now());
        return enrichWithPatientNames(allocationMapper.toDtoList(allocations));
    }

    /**
     * Get all coupons assigned to a patient (any status)
     */
    @Transactional(readOnly = true)
    public List<SupervisorCouponAllocationDto> getAllCouponsForPatient(
            Long supervisorUserId, Long patientId) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        
        validatePatientAssignment(supervisor.getId(), patientId);

        List<SupervisorCouponAllocation> allocations = allocationRepository
                .findBySupervisorIdAndAssignedPatientIdOrderByCreatedAtDesc(supervisor.getId(), patientId);
        return enrichWithPatientNames(allocationMapper.toDtoList(allocations));
    }

    /**
     * Get coupons expiring soon
     */
    @Transactional(readOnly = true)
    public List<SupervisorCouponAllocationDto> getExpiringCoupons(Long supervisorUserId, int days) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        
        List<SupervisorCouponAllocation> allocations = allocationRepository
                .findExpiringSoonForSupervisor(supervisor.getId(), now, futureDate);
        return enrichWithPatientNames(allocationMapper.toDtoList(allocations));
    }

    /**
     * Get coupon by code
     */
    @Transactional(readOnly = true)
    public SupervisorCouponAllocationDto getCouponByCode(Long supervisorUserId, String couponCode) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        
        SupervisorCouponAllocation allocation = allocationRepository
                .findByCouponCodeAndSupervisorId(couponCode.toUpperCase(), supervisor.getId())
                .orElseThrow(() -> new BusinessException("Coupon not found", HttpStatus.NOT_FOUND));
        
        return enrichWithPatientName(allocationMapper.toDto(allocation));
    }

    // ==================== Coupon Summary ====================

    /**
     * Get coupon summary for supervisor
     */
    @Transactional(readOnly = true)
    public CouponSummaryDto getCouponSummary(Long supervisorUserId) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        LocalDateTime now = LocalDateTime.now();

        long total = allocationRepository.countBySupervisorId(supervisor.getId());
        long available = allocationRepository.countBySupervisorIdAndStatus(supervisor.getId(), SupervisorCouponStatus.AVAILABLE);
        long assigned = allocationRepository.countBySupervisorIdAndStatus(supervisor.getId(), SupervisorCouponStatus.ASSIGNED);
        long used = allocationRepository.countBySupervisorIdAndStatus(supervisor.getId(), SupervisorCouponStatus.USED);
        long expired = allocationRepository.countBySupervisorIdAndStatus(supervisor.getId(), SupervisorCouponStatus.EXPIRED);
        long cancelled = allocationRepository.countBySupervisorIdAndStatus(supervisor.getId(), SupervisorCouponStatus.CANCELLED);
        
        BigDecimal availableValue = allocationRepository.sumAvailableValueForSupervisor(supervisor.getId(), now);
        
        List<SupervisorCouponAllocation> expiringSoon = allocationRepository
                .findExpiringSoonForSupervisor(supervisor.getId(), now, now.plusDays(30));

        return CouponSummaryDto.builder()
                .totalCoupons((int) total)
                .createdCoupons(0) // Not applicable for supervisor
                .distributedCoupons((int) (available + assigned))
                .availableCoupons((int) available)
                .usedCoupons((int) used)
                .assignedCoupons((int) assigned)
                .expiredCoupons((int) expired)
                .cancelledCoupons((int) cancelled)
                .expiringSoonCoupons(expiringSoon.size())
                .totalAvailableValue(availableValue != null ? availableValue : BigDecimal.ZERO)
                .build();
    }

    /**
     * Get coupon summary for a specific patient
     */
    @Transactional(readOnly = true)
    public CouponSummaryDto getCouponSummaryForPatient(Long supervisorUserId, Long patientId) {
        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);
        validatePatientAssignment(supervisor.getId(), patientId);

        LocalDateTime now = LocalDateTime.now();
        
        long available = allocationRepository.countAvailableCouponsForPatient(
                supervisor.getId(), patientId, now);

        List<SupervisorCouponAllocation> patientCoupons = allocationRepository
                .findBySupervisorIdAndAssignedPatientIdOrderByCreatedAtDesc(supervisor.getId(), patientId);

        long used = patientCoupons.stream()
                .filter(c -> c.getStatus() == SupervisorCouponStatus.USED)
                .count();
        long expired = patientCoupons.stream()
                .filter(c -> c.getStatus() == SupervisorCouponStatus.EXPIRED)
                .count();
        long expiringSoon = patientCoupons.stream()
                .filter(SupervisorCouponAllocation::isExpiringSoon)
                .count();

        BigDecimal availableValue = patientCoupons.stream()
                .filter(c -> c.getStatus() == SupervisorCouponStatus.ASSIGNED && !c.isExpired())
                .map(SupervisorCouponAllocation::getDiscountValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CouponSummaryDto.builder()
                .totalCoupons(patientCoupons.size())
                .distributedCoupons((int) available)
                .usedCoupons((int) used)
                .expiredCoupons((int) expired)
                .expiringSoonCoupons((int) expiringSoon)
                .totalAvailableValue(availableValue)
                .build();
    }

    // ==================== Sync with Admin Service ====================

    /**
     * Sync local allocations with admin-service.
     * Called to refresh local data if out of sync.
     */
    public void syncWithAdminService(Long supervisorUserId) {
        log.info("Syncing coupon allocations with admin-service for supervisor user {}", supervisorUserId);

        MedicalSupervisor supervisor = getSupervisorByUserId(supervisorUserId);

        try {
            ResponseEntity<ApiResponse<List<AdminCouponDto>>> response = 
                    adminCouponClient.getCouponsForBeneficiary(
                            BeneficiaryType.MEDICAL_SUPERVISOR, supervisor.getId());

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<AdminCouponDto> adminCoupons = response.getBody().getData();
                
                for (AdminCouponDto adminCoupon : adminCoupons) {
                    updateLocalAllocationFromAdmin(supervisor.getId(), adminCoupon);
                }
                
                log.info("Synced {} coupons from admin-service", adminCoupons.size());
            }
        } catch (Exception e) {
            log.error("Failed to sync with admin-service: {}", e.getMessage(), e);
            throw new BusinessException("Failed to sync coupon data", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void updateLocalAllocationFromAdmin(Long supervisorId, AdminCouponDto adminCoupon) {
        allocationRepository.findByAdminCouponId(adminCoupon.getId())
                .ifPresent(allocation -> {
                    // Update status from admin
                    SupervisorCouponStatus newStatus = mapAdminStatusToLocal(adminCoupon.getStatus());
                    if (allocation.getStatus() != newStatus) {
                        allocation.setStatus(newStatus);
                        allocation.setLastSyncedAt(LocalDateTime.now());
                        
                        if (adminCoupon.getUsedAt() != null) {
                            allocation.setUsedAt(adminCoupon.getUsedAt());
                            allocation.setUsedForCaseId(adminCoupon.getUsedForCaseId());
                            allocation.setUsedForPaymentId(adminCoupon.getUsedForPaymentId());
                        }
                        
                        allocationRepository.save(allocation);
                    }
                });
    }

    private SupervisorCouponStatus mapAdminStatusToLocal(com.commonlibrary.entity.AdminCouponStatus adminStatus) {
        return switch (adminStatus) {
            case CREATED, DISTRIBUTED -> SupervisorCouponStatus.AVAILABLE;
            case USED -> SupervisorCouponStatus.USED;
            case EXPIRED -> SupervisorCouponStatus.EXPIRED;
            case CANCELLED, SUSPENDED -> SupervisorCouponStatus.CANCELLED;
        };
    }

    // ==================== Scheduled Tasks ====================

    /**
     * Update expired coupons status.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void updateExpiredCoupons() {
        log.info("Running scheduled task to update expired coupons");
        int updated = allocationRepository.updateExpiredCoupons(LocalDateTime.now());
        log.info("Updated {} expired coupon allocations", updated);
    }

    // ==================== Helper Methods ====================

    private MedicalSupervisor getSupervisorByUserId(Long userId) {
        return supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
    }

    private void validatePatientAssignment(Long supervisorId, Long patientId) {
        boolean isAssigned = assignmentRepository
                .existsBySupervisorIdAndPatientIdAndAssignmentStatus(
                        supervisorId, patientId, SupervisorAssignmentStatus.ACTIVE);
        
        if (!isAssigned) {
            throw new BusinessException("Patient is not assigned to this supervisor", HttpStatus.FORBIDDEN);
        }
    }

    private SupervisorCouponAllocationDto enrichWithPatientName(SupervisorCouponAllocationDto dto) {
        if (dto == null || dto.getAssignedPatientId() == null) {
            return dto;
        }

        try {
            // Get patient name from patient-service
            String patientName = getPatientName(dto.getAssignedPatientId());
            dto.setAssignedPatientName(patientName);
        } catch (Exception e) {
            log.warn("Could not fetch patient name for ID {}: {}", dto.getAssignedPatientId(), e.getMessage());
        }

        return dto;
    }

    private List<SupervisorCouponAllocationDto> enrichWithPatientNames(List<SupervisorCouponAllocationDto> dtos) {
        return dtos.stream()
                .map(this::enrichWithPatientName)
                .collect(Collectors.toList());
    }

    private String getPatientName(Long patientId) {
        try {
            ResponseEntity<ApiResponse<PatientProfileDto>> response = patientServiceClient.getPatientById(patientId);
            if (response.getBody() != null && response.getBody().getData() != null) {
                PatientProfileDto patientProfileDto = response.getBody().getData();
                return "Patient " + patientProfileDto.getFullName();
            }
        } catch (Exception e) {
            log.debug("Could not fetch patient {}: {}", patientId, e.getMessage());
        }
        return null;
    }
}