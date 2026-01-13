package com.adminservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.BeneficiaryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for tracking coupon redemption history.
 * Provides audit trail for all coupon usages.
 */
@Entity
@Table(name = "coupon_redemption_history", indexes = {
    @Index(name = "idx_redemption_coupon", columnList = "couponId"),
    @Index(name = "idx_redemption_case", columnList = "caseId"),
    @Index(name = "idx_redemption_patient", columnList = "patientId"),
    @Index(name = "idx_redemption_payment", columnList = "paymentId"),
    @Index(name = "idx_redemption_date", columnList = "redeemedAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemptionHistory extends BaseEntity {

    /**
     * Reference to the coupon that was redeemed
     */
    @Column(nullable = false)
    private Long couponId;

    /**
     * Coupon code (denormalized for quick lookup)
     */
    @Column(nullable = false, length = 50)
    private String couponCode;

    // ==================== Who Redeemed ====================

    /**
     * Type of entity that redeemed (MEDICAL_SUPERVISOR or PATIENT)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryType redeemedByType;

    /**
     * ID of the entity that redeemed (supervisor ID or patient ID)
     */
    @Column(nullable = false)
    private Long redeemedById;

    /**
     * User ID of the person who redeemed
     */
    @Column(nullable = false)
    private Long redeemedByUserId;

    // ==================== For Whom ====================

    /**
     * Patient ID the coupon was used for
     */
    @Column(nullable = false)
    private Long patientId;

    /**
     * Case ID the coupon was used for
     */
    @Column(nullable = false)
    private Long caseId;

    /**
     * Payment ID created for this transaction
     */
    @Column(nullable = false)
    private Long paymentId;

    // ==================== Amounts ====================

    /**
     * Original consultation fee before discount
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    /**
     * Discount amount applied
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountApplied;

    /**
     * Final amount charged (via Stripe if any)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    /**
     * Currency code
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    // ==================== Timestamp ====================

    /**
     * When the coupon was redeemed
     */
    @Column(nullable = false)
    private LocalDateTime redeemedAt;
}