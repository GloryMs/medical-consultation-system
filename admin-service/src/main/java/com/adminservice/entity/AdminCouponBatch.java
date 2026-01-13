package com.adminservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.CouponBatchStatus;
import com.commonlibrary.entity.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for managing batch coupon creation.
 * Allows admin to create multiple coupons at once with the same configuration.
 */
@Entity
@Table(name = "admin_coupon_batches", indexes = {
    @Index(name = "idx_batch_code", columnList = "batchCode"),
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_beneficiary", columnList = "beneficiaryType, beneficiaryId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCouponBatch extends BaseEntity {

    /**
     * Unique batch code (e.g., "BATCH-2026-001")
     */
    @Column(nullable = false, unique = true, length = 50)
    private String batchCode;

    // ==================== Batch Configuration ====================

    /**
     * Total number of coupons in this batch
     */
    @Column(nullable = false)
    private Integer totalCoupons;

    /**
     * Type of discount for all coupons in batch
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    /**
     * Discount value for all coupons
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Maximum discount amount (for percentage discounts)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * Currency code
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    // ==================== Beneficiary Information ====================

    /**
     * Type of beneficiary for all coupons
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryType beneficiaryType;

    /**
     * Target beneficiary ID (null if coupons are for pool)
     */
    private Long beneficiaryId;

    // ==================== Expiration Configuration ====================

    /**
     * Number of days until coupons expire (from creation or distribution)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer expiryDays = 180;

    /**
     * Whether coupons can be transferred between patients
     */
    @Builder.Default
    private Boolean isTransferable = true;

    // ==================== Status Tracking ====================

    /**
     * Batch status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CouponBatchStatus status = CouponBatchStatus.CREATED;

    // ==================== Admin Tracking ====================

    /**
     * Admin user ID who created the batch
     */
    @Column(nullable = false)
    private Long createdBy;

    /**
     * Admin user ID who distributed the batch
     */
    private Long distributedBy;

    /**
     * When the batch was distributed
     */
    private LocalDateTime distributedAt;

    // ==================== Notes ====================

    /**
     * Notes about the batch
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ==================== Soft Delete ====================

    /**
     * Soft delete flag
     */
    @Builder.Default
    private Boolean isDeleted = false;
}