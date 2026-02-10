package com.supervisorservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a batch of coupons issued together
 */
@Entity
@Table(name = "coupon_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "batch_code", nullable = false, unique = true, length = 50)
    private String batchCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private MedicalSupervisor supervisor;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @Column(name = "total_coupons", nullable = false)
    private Integer totalCoupons;
    
    @Column(name = "amount_per_coupon", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPerCoupon;
    
    @Column(name = "expiry_months", nullable = false)
    @Builder.Default
    private Integer expiryMonths = 6;
    
    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    /**
     * Calculate total batch value
     */
    @Transient
    public BigDecimal getTotalBatchValue() {
        return amountPerCoupon.multiply(BigDecimal.valueOf(totalCoupons));
    }
}
