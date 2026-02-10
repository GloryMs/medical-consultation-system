package com.supervisorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.CouponStatus;
import com.commonlibrary.entity.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a coupon for case payment
 */
@Entity
@Table(name = "supervisor_coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SupervisorCoupon extends BaseEntity {
    
    @Column(name = "coupon_code", nullable = false, unique = true, length = 50)
    private String couponCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private MedicalSupervisor supervisor;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "case_id")
    private Long caseId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private CouponStatus status = CouponStatus.AVAILABLE;
    
    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private CouponBatch batch;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Denormalized patient name for display
     */
    @Column(name = "patient_name")
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    /**
     * Discount value:
     * - For PERCENTAGE: the percentage (e.g., 20 for 20%)
     * - For FIXED_AMOUNT: the dollar amount (e.g., 50.00)
     * - For FULL_COVERAGE: not used (can be null or 100)
     */
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Maximum discount amount (for PERCENTAGE type)
     * Null means no cap
     */
    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "used_for_case_id")
    private Long usedForCaseId;

    /**
     * Payment ID created when coupon was redeemed
     */
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * Transaction ID for the redemption
     */
    @Column(name = "transaction_id")
    private String transactionId;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Check if coupon is usable
     */
    @Transient
    public boolean isUsable() {
        return status == CouponStatus.AVAILABLE
                && !isDeleted
                && expiresAt.isAfter(LocalDateTime.now());
    }
    
    /**
     * Check if coupon is expired
     */
    @Transient
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now())
                && status == CouponStatus.AVAILABLE;
    }
    
    /**
     * Check if coupon is expiring soon (within warning days)
     */
    @Transient
    public boolean isExpiringSoon(int warningDays) {
        if (status != CouponStatus.AVAILABLE || isDeleted) {
            return false;
        }
        LocalDateTime warningDate = LocalDateTime.now().plusDays(warningDays);
        return expiresAt.isBefore(warningDate) && expiresAt.isAfter(LocalDateTime.now());
    }
    
    /**
     * Mark coupon as used
     */
    public void markAsUsed(Long caseId) {
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.caseId = caseId;
    }
    
    /**
     * Mark coupon as expired
     */
    public void markAsExpired() {
        if (this.status == CouponStatus.AVAILABLE) {
            this.status = CouponStatus.EXPIRED;
        }
    }
    
    /**
     * Cancel coupon
     */
    public void cancel(String reason) {
        this.status = CouponStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }



    /**
     * Check if coupon is available for use
     */
    public boolean isAvailable() {
        return status == CouponStatus.AVAILABLE && !isExpired() && !isDeleted;
    }

    /**
     * Check if coupon is expiring soon (within 7 days)
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) return false;
        return expiresAt.isBefore(LocalDateTime.now().plusDays(7)) &&
                expiresAt.isAfter(LocalDateTime.now());
    }
}
