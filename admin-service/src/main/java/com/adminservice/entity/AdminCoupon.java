package com.adminservice.entity;

import com.commonlibrary.entity.AdminCouponStatus;
import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Master coupon entity - Source of truth for all coupons.
 * Admin creates and manages coupons here, then distributes to supervisors or patients.
 */
@Entity
@Table(name = "admin_coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "couponCode"),
    @Index(name = "idx_coupon_status", columnList = "status"),
    @Index(name = "idx_coupon_beneficiary", columnList = "beneficiaryType, beneficiaryId"),
    @Index(name = "idx_coupon_batch", columnList = "batchId"),
    @Index(name = "idx_coupon_expires", columnList = "expiresAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCoupon extends BaseEntity {

    /**
     * Unique coupon code (e.g., "MED-2026-ABC123")
     */
    @Column(nullable = false, unique = true, length = 50)
    private String couponCode;

    // ==================== Discount Configuration ====================

    /**
     * Type of discount: PERCENTAGE, FIXED_AMOUNT, or FULL_COVERAGE
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    /**
     * Discount value - percentage (1-100) or fixed amount
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Maximum discount amount (cap for percentage discounts)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * Currency code (default: USD)
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    // ==================== Beneficiary Information ====================

    /**
     * Type of beneficiary: MEDICAL_SUPERVISOR or PATIENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryType beneficiaryType;

    /**
     * Beneficiary ID (supervisor or patient ID)
     * Null if coupon is in pool (not yet assigned)
     */
    private Long beneficiaryId;

    // ==================== Status Tracking ====================

    /**
     * Current status of the coupon
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AdminCouponStatus status = AdminCouponStatus.CREATED;

    // ==================== Batch Reference ====================

    /**
     * Reference to batch if created as part of batch
     */
    private Long batchId;

    // ==================== Admin Tracking ====================

    /**
     * Admin user ID who created the coupon
     */
    @Column(nullable = false)
    private Long createdBy;

    /**
     * Admin user ID who distributed the coupon
     */
    private Long distributedBy;

    /**
     * When the coupon was distributed to beneficiary
     */
    private LocalDateTime distributedAt;

    // ==================== Usage Tracking ====================

    /**
     * When the coupon was used (redeemed)
     */
    private LocalDateTime usedAt;

    /**
     * Case ID the coupon was used for
     */
    private Long usedForCaseId;

    /**
     * Payment ID the coupon was used for
     */
    private Long usedForPaymentId;

    /**
     * Patient ID the coupon was used for (may differ from beneficiary if supervisor used it)
     */
    private Long usedByPatientId;

    // ==================== Expiration ====================

    /**
     * When the coupon expires
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // ==================== Transferability ====================

    /**
     * Whether supervisor can transfer/reassign to different patients
     */
    @Builder.Default
    private Boolean isTransferable = true;

    // ==================== Notes & Cancellation ====================

    /**
     * Notes about the coupon
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Reason for cancellation (if cancelled)
     */
    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * When the coupon was cancelled
     */
    private LocalDateTime cancelledAt;

    /**
     * Admin user ID who cancelled the coupon
     */
    private Long cancelledBy;

    // ==================== Soft Delete ====================

    /**
     * Soft delete flag
     */
    @Builder.Default
    private Boolean isDeleted = false;

    // ==================== Helper Methods ====================

    /**
     * Check if coupon is available for use
     */
    public boolean isAvailable() {
        return status == AdminCouponStatus.DISTRIBUTED 
            && !isExpired() 
            && !Boolean.TRUE.equals(isDeleted);
    }

    /**
     * Check if coupon is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if coupon is expiring soon (within 30 days)
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) return false;
        LocalDateTime thirtyDaysFromNow = LocalDateTime.now().plusDays(30);
        return expiresAt.isBefore(thirtyDaysFromNow) && !isExpired();
    }

    /**
     * Get days until expiry
     */
    public int getDaysUntilExpiry() {
        if (expiresAt == null || isExpired()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    /**
     * Calculate discount amount for a given fee
     */
    public BigDecimal calculateDiscount(BigDecimal consultationFee) {
        if (consultationFee == null || consultationFee.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        switch (discountType) {
            case PERCENTAGE:
                discount = consultationFee.multiply(discountValue).divide(BigDecimal.valueOf(100));
                // Apply cap if set
                if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                    discount = maxDiscountAmount;
                }
                break;
            case FIXED_AMOUNT:
                discount = discountValue;
                // Cannot exceed consultation fee
                if (discount.compareTo(consultationFee) > 0) {
                    discount = consultationFee;
                }
                break;
            case FULL_COVERAGE:
                discount = consultationFee;
                break;
            default:
                discount = BigDecimal.ZERO;
        }
        return discount;
    }
}