package com.supervisorservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a coupon redemption record
 */
@Entity
@Table(name = "coupon_redemptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRedemption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private SupervisorCoupon coupon;
    
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    
    @Column(name = "case_id", nullable = false)
    private Long caseId;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private MedicalSupervisor supervisor;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "redeemed_at", nullable = false)
    @Builder.Default
    private LocalDateTime redeemedAt = LocalDateTime.now();
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (redeemedAt == null) {
            redeemedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
