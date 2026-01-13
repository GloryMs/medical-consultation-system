package com.adminservice.mapper;

import com.adminservice.entity.AdminCoupon;
import com.adminservice.entity.AdminCouponBatch;
import com.adminservice.entity.CouponRedemptionHistory;
import com.commonlibrary.dto.coupon.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between coupon entities and DTOs.
 */
@Component
public class AdminCouponMapper {

    // ==================== AdminCoupon Mappings ====================

    /**
     * Convert AdminCoupon entity to AdminCouponDto
     */
    public AdminCouponDto toDto(AdminCoupon entity) {
        if (entity == null) return null;

        return AdminCouponDto.builder()
                .id(entity.getId())
                .couponCode(entity.getCouponCode())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .currency(entity.getCurrency())
                .beneficiaryType(entity.getBeneficiaryType())
                .beneficiaryId(entity.getBeneficiaryId())
                .status(entity.getStatus())
                .batchId(entity.getBatchId())
                .createdBy(entity.getCreatedBy())
                .distributedBy(entity.getDistributedBy())
                .distributedAt(entity.getDistributedAt())
                .usedAt(entity.getUsedAt())
                .usedForCaseId(entity.getUsedForCaseId())
                .usedForPaymentId(entity.getUsedForPaymentId())
                .usedByPatientId(entity.getUsedByPatientId())
                .expiresAt(entity.getExpiresAt())
                .isExpiringSoon(entity.isExpiringSoon())
                .daysUntilExpiry(entity.getDaysUntilExpiry())
                .isTransferable(entity.getIsTransferable())
                .notes(entity.getNotes())
                .cancellationReason(entity.getCancellationReason())
                .cancelledAt(entity.getCancelledAt())
                .cancelledBy(entity.getCancelledBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of AdminCoupon entities to DTOs
     */
    public List<AdminCouponDto> toDtoList(List<AdminCoupon> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert CreateCouponRequest to AdminCoupon entity
     */
    public AdminCoupon toEntity(CreateCouponRequest request, Long adminUserId) {
        if (request == null) return null;

        return AdminCoupon.builder()
                .couponCode(request.getCouponCode())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .beneficiaryType(request.getBeneficiaryType())
                .beneficiaryId(request.getBeneficiaryId())
                .expiresAt(request.getExpiresAt())
                .isTransferable(request.getIsTransferable() != null ? request.getIsTransferable() : true)
                .notes(request.getNotes())
                .createdBy(adminUserId)
                .isDeleted(false)
                .build();
    }

    // ==================== AdminCouponBatch Mappings ====================

    /**
     * Convert AdminCouponBatch entity to CouponBatchDto
     */
    public CouponBatchDto toBatchDto(AdminCouponBatch entity) {
        if (entity == null) return null;

        return CouponBatchDto.builder()
                .id(entity.getId())
                .batchCode(entity.getBatchCode())
                .beneficiaryType(entity.getBeneficiaryType())
                .beneficiaryId(entity.getBeneficiaryId())
                .totalCoupons(entity.getTotalCoupons())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .currency(entity.getCurrency())
                .expiryDays(entity.getExpiryDays())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .distributedBy(entity.getDistributedBy())
                .distributedAt(entity.getDistributedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of AdminCouponBatch entities to DTOs
     */
    public List<CouponBatchDto> toBatchDtoList(List<AdminCouponBatch> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(this::toBatchDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert CreateBatchCouponsRequest to AdminCouponBatch entity
     */
    public AdminCouponBatch toBatchEntity(CreateBatchCouponsRequest request, Long adminUserId) {
        if (request == null) return null;

        return AdminCouponBatch.builder()
                .totalCoupons(request.getTotalCoupons())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .beneficiaryType(request.getBeneficiaryType())
                .beneficiaryId(request.getBeneficiaryId())
                .expiryDays(request.getExpiryDays() != null ? request.getExpiryDays() : 180)
                .isTransferable(request.getIsTransferable() != null ? request.getIsTransferable() : true)
                .notes(request.getNotes())
                .createdBy(adminUserId)
                .isDeleted(false)
                .build();
    }

    // ==================== CouponRedemptionHistory Mappings ====================

    /**
     * Create CouponRedemptionHistory from mark used request
     */
    public CouponRedemptionHistory toRedemptionHistory(
            AdminCoupon coupon,
            MarkCouponUsedRequest request,
            com.commonlibrary.entity.BeneficiaryType redeemerType) {
        
        if (coupon == null || request == null) return null;

        return CouponRedemptionHistory.builder()
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .redeemedByType(redeemerType)
                .redeemedById(coupon.getBeneficiaryId())
                .redeemedByUserId(request.getRedeemedByUserId())
                .patientId(request.getPatientId())
                .caseId(request.getCaseId())
                .paymentId(request.getPaymentId())
                .originalAmount(request.getDiscountApplied().add(request.getAmountCharged()))
                .discountApplied(request.getDiscountApplied())
                .finalAmount(request.getAmountCharged())
                .currency(coupon.getCurrency())
                .redeemedAt(request.getUsedAt() != null ? request.getUsedAt() : LocalDateTime.now())
                .build();
    }

    // ==================== Event Mappings ====================

    /**
     * Create CouponDistributionEvent from AdminCoupon
     */
    public CouponDistributionEvent toDistributionEvent(AdminCoupon coupon) {
        if (coupon == null) return null;

        return CouponDistributionEvent.builder()
                .eventType("COUPON_DISTRIBUTED")
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .beneficiaryType(coupon.getBeneficiaryType())
                .beneficiaryId(coupon.getBeneficiaryId())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .currency(coupon.getCurrency())
                .expiresAt(coupon.getExpiresAt())
                .isTransferable(coupon.getIsTransferable())
                .batchId(coupon.getBatchId())
                .distributedBy(coupon.getDistributedBy())
                .notes(coupon.getNotes())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create CouponUsedEvent from AdminCoupon and request
     */
    public CouponUsedEvent toUsedEvent(AdminCoupon coupon, MarkCouponUsedRequest request) {
        if (coupon == null || request == null) return null;

        return CouponUsedEvent.builder()
                .eventType("COUPON_USED")
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .beneficiaryType(coupon.getBeneficiaryType())
                .beneficiaryId(coupon.getBeneficiaryId())
                .patientId(request.getPatientId())
                .caseId(request.getCaseId())
                .paymentId(request.getPaymentId())
                .originalAmount(request.getDiscountApplied().add(request.getAmountCharged()))
                .discountAmount(request.getDiscountApplied())
                .chargedAmount(request.getAmountCharged())
                .currency(coupon.getCurrency())
                .usedAt(request.getUsedAt() != null ? request.getUsedAt() : LocalDateTime.now())
                .redeemedByUserId(request.getRedeemedByUserId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create CouponCancelledEvent from AdminCoupon
     */
    public CouponCancelledEvent toCancelledEvent(AdminCoupon coupon) {
        if (coupon == null) return null;

        return CouponCancelledEvent.builder()
                .eventType("COUPON_CANCELLED")
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .beneficiaryType(coupon.getBeneficiaryType())
                .beneficiaryId(coupon.getBeneficiaryId())
                .cancellationReason(coupon.getCancellationReason())
                .cancelledBy(coupon.getCancelledBy())
                .cancelledAt(coupon.getCancelledAt())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== Validation Response Mapping ====================

    /**
     * Create CouponValidationResponse from AdminCoupon
     */
    public CouponValidationResponse toValidationResponse(
            AdminCoupon coupon, 
            java.math.BigDecimal consultationFee,
            boolean isValid,
            String message,
            String errorCode) {
        
        if (coupon == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message(message)
                    .errorCode(errorCode)
                    .build();
        }

        java.math.BigDecimal discountAmount = isValid ? coupon.calculateDiscount(consultationFee) : null;
        java.math.BigDecimal remainingAmount = isValid && discountAmount != null 
                ? consultationFee.subtract(discountAmount) 
                : null;

        return CouponValidationResponse.builder()
                .valid(isValid)
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .discountAmount(discountAmount)
                .remainingAmount(remainingAmount)
                .originalAmount(consultationFee)
                .currency(coupon.getCurrency())
                .expiresAt(coupon.getExpiresAt())
                .patientId(coupon.getUsedByPatientId())
                .beneficiaryId(coupon.getBeneficiaryId())
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}