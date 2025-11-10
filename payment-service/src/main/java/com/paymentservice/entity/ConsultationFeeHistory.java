package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Consultation Fee History entity for maintaining audit trail of fee changes
 */
@Entity
@Table(name = "consultation_fee_history", indexes = {
    @Index(name = "idx_specialization_history", columnList = "specialization"),
    @Index(name = "idx_effective_date_history", columnList = "effective_date"),
    @Index(name = "idx_changed_by", columnList = "changed_by")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationFeeHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String specialization;
    
    @Column(name = "old_fee", precision = 10, scale = 2)
    private BigDecimal oldFee;
    
    @Column(name = "new_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal newFee;
    
    @Column(name = "changed_by", nullable = false)
    private Long changedBy; // Admin user ID
    
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (effectiveDate == null) {
            effectiveDate = LocalDateTime.now();
        }
    }
}