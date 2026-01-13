package com.supervisorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.DiscountType;
import com.commonlibrary.entity.SupervisorCouponStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Local coupon allocation tracking in supervisor-service.
 * This is a local copy of coupon data received from admin-service via Kafka.
 * The source of truth remains in admin-service.
 */
@Entity
@Table(name = "supervisor_coupon_allocations", indexes = {
    @Index(name = "idx_allocation_coupon_code", columnList = "couponCode"),
    @Index(name = "idx_allocation_supervisor", columnList = "supervisorId"),
    @Index(name = "idx_allocation_patient", columnList = "assignedPatientId"),
    @Index(name = "idx_allocation_status", columnList = "status"),
    @Index(name = "idx_allocation_admin_coupon", columnList = "adminCouponId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_coupon_code_supervisor", columnNames = {"couponCode", "supervisorId"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorCouponAllocation extends BaseEntity {

    /**
     * Reference to the coupon ID in admin-service (logical foreign key)
     */
    @Column(nullable = false)
    private Long adminCouponId;

    /**
     * Coupon code (denormalized from admin-service for quick access)
     */
    @Column(nullable = false, length = 50)
    private String couponCode;

    /**
     * Supervisor ID who owns this allocation
     */
    @Column(nullable = false)
    private Long supervisorId;

    // ==================== Assignment to Patient ====================

    /**
     * Patient ID this coupon is assigned to (null if unassigned)
     */
    private Long assignedPatientId;

    /**
     * When the coupon was assigned to the patient
     */
    private LocalDateTime assignedAt;

    /**
     * Notes about the assignment
     */
    @Column(columnDefinition = "TEXT")
    private String assignmentNotes;

    // ==================== Discount Info (copied from admin for fast access) ====================

    /**
     * Type of discount
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    /**
     * Discount value
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Maximum discount amount
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * Currency code
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    // ==================== Status Tracking ====================

    /**
     * Local status of the coupon
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SupervisorCouponStatus status = SupervisorCouponStatus.AVAILABLE;

    // ==================== Usage Tracking ====================

    /**
     * When the coupon was used
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

    // ==================== Expiration ====================

    /**
     * When the coupon expires
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // ==================== Sync Tracking ====================

    /**
     * When the coupon was received from admin-service
     */
    @Column(nullable = false)
    private LocalDateTime receivedAt;

    /**
     * Last synchronization time with admin-service
     */
    private LocalDateTime lastSyncedAt;

    // ==================== Batch Reference ====================

    /**
     * Batch ID if part of a batch (from admin-service)
     */
    private Long batchId;

    /**
     * Batch code if part of a batch
     */
    private String batchCode;

    // ==================== Helper Methods ====================

    /**
     * Check if coupon is available for assignment
     */
    public boolean isAvailableForAssignment() {
        return status == SupervisorCouponStatus.AVAILABLE 
            && assignedPatientId == null 
            && !isExpired();
    }

    /**
     * Check if coupon is available for use (assigned and not used)
     */
    public boolean isAvailableForUse() {
        return status == SupervisorCouponStatus.ASSIGNED 
            && assignedPatientId != null 
            && !isExpired();
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
                if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                    discount = maxDiscountAmount;
                }
                break;
            case FIXED_AMOUNT:
                discount = discountValue;
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