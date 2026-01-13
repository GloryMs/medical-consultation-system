package com.patientservice.service;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PatientCouponAllocationDto;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import com.patientservice.feign.AdminServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for patient coupon operations.
 * Allows patients to view and use their available coupons.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientCouponService {

    private final AdminServiceClient adminCouponClient;

    /**
     * Get available coupons for a patient.
     * These are coupons that can be used for payment.
     */
    @Transactional(readOnly = true)
    public List<PatientCouponAllocationDto> getAvailableCoupons(Long patientId) {
        log.info("Fetching available coupons for patient {}", patientId);

        try {
            ResponseEntity<ApiResponse<List<AdminCouponDto>>> response = 
                    adminCouponClient.getAvailableCouponsForBeneficiary(
                            BeneficiaryType.PATIENT, patientId);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData().stream()
                        .map(this::toPatientDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching available coupons for patient {}: {}", patientId, e.getMessage());
        }

        return Collections.emptyList();
    }

//    /**
//     * Get all coupons for a patient (any status).
//     */
//    @Transactional(readOnly = true)
//    public List<PatientCouponAllocationDto> getAllCoupons(Long patientId) {
//        log.info("Fetching all coupons for patient {}", patientId);
//
//        try {
//            ResponseEntity<ApiResponse<List<AdminCouponDto>>> response =
//                    adminCouponClient.getCoupons(
//                            BeneficiaryType.PATIENT, patientId);
//
//            if (response.getBody() != null && response.getBody().getData() != null) {
//                return response.getBody().getData().stream()
//                        .map(this::toPatientDto)
//                        .collect(Collectors.toList());
//            }
//        } catch (Exception e) {
//            log.error("Error fetching coupons for patient {}: {}", patientId, e.getMessage());
//        }
//
//        return Collections.emptyList();
//    }

    @Transactional(readOnly = true)
    public List<PatientCouponAllocationDto> getAllCoupons(Long patientId) {
        log.info("Fetching all coupons for patient {}", patientId);

        try {
            ResponseEntity<ApiResponse<Page<AdminCouponDto>>> response =
                    adminCouponClient.getCoupons(BeneficiaryType.PATIENT, patientId);

            if (response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    response.getBody().getData() != null) {

                Page<AdminCouponDto> page = response.getBody().getData();

                return page.getContent().stream()
                        .map(this::toPatientDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching coupons for patient {}: {}", patientId, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * Get coupon summary for a patient.
     */
    @Transactional(readOnly = true)
    public CouponSummaryDto getCouponSummary(Long patientId) {
        log.info("Fetching coupon summary for patient {}", patientId);

        try {
            ResponseEntity<ApiResponse<CouponSummaryDto>> response = 
                    adminCouponClient.getCouponSummaryForBeneficiary(
                            BeneficiaryType.PATIENT, patientId);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Error fetching coupon summary for patient {}: {}", patientId, e.getMessage());
        }

        return CouponSummaryDto.builder()
                .totalCoupons(0)
                .distributedCoupons(0)
                .usedCoupons(0)
                .expiredCoupons(0)
                .totalAvailableValue(BigDecimal.ZERO)
                .build();
    }

    /**
     * Validate a coupon for use by patient.
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(
            Long patientId, String couponCode, Long caseId, BigDecimal amount) {
        
        log.info("Validating coupon {} for patient {} on case {}", couponCode, patientId, caseId);

        try {
            CouponValidationRequest request = CouponValidationRequest.builder()
                    .couponCode(couponCode.toUpperCase().trim())
                    .beneficiaryType(BeneficiaryType.PATIENT)
                    .beneficiaryId(patientId)
                    .patientId(patientId)
                    .caseId(caseId)
                    .requestedAmount(amount)
                    .build();

            ResponseEntity<ApiResponse<CouponValidationResponse>> response = 
                    adminCouponClient.validateCoupon(request);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Error validating coupon {} for patient {}: {}", 
                    couponCode, patientId, e.getMessage());
        }

        return CouponValidationResponse.builder()
                .valid(false)
                .couponCode(couponCode)
                .message("Could not validate coupon")
                .build();
    }

    /**
     * Get coupon details by code.
     */
    @Transactional(readOnly = true)
    public PatientCouponAllocationDto getCouponByCode(String couponCode) {
        log.info("Fetching coupon by code {}", couponCode);

        try {
            ResponseEntity<ApiResponse<AdminCouponDto>> response = 
                    adminCouponClient.getCouponByCode(couponCode);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return toPatientDto(response.getBody().getData());
            }
        } catch (Exception e) {
            log.error("Error fetching coupon {}: {}", couponCode, e.getMessage());
        }

        return null;
    }

    // ==================== Helper Methods ====================

    private PatientCouponAllocationDto toPatientDto(AdminCouponDto adminDto) {
        if (adminDto == null) return null;

        boolean isAvailable = "DISTRIBUTED".equals(adminDto.getStatus().name()) 
                && !adminDto.getIsExpiringSoon();

        return PatientCouponAllocationDto.builder()
                .id(adminDto.getId())
                .couponCode(adminDto.getCouponCode())
                .patientId(adminDto.getBeneficiaryId())
                .discountType(adminDto.getDiscountType())
                .discountValue(adminDto.getDiscountValue())
                .maxDiscountAmount(adminDto.getMaxDiscountAmount())
                .currency(adminDto.getCurrency())
                .available(isAvailable)
                .assignedAt(adminDto.getDistributedAt())
                .expiresAt(adminDto.getExpiresAt())
                .expiringSoon(adminDto.getIsExpiringSoon())
                .daysUntilExpiry(adminDto.getDaysUntilExpiry())
                .used("USED".equals(adminDto.getStatus().name()))
                .usedAt(adminDto.getUsedAt())
                .usedForCaseId(adminDto.getUsedForCaseId())
                .build();
    }
}