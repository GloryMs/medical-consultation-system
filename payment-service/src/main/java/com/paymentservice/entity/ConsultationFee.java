package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Consultation Fee entity for managing specialization-based consultation fees
 */
@Entity
@Table(name = "consultation_fees", indexes = {
    @Index(name = "idx_specialization", columnList = "specialization", unique = true),
    @Index(name = "idx_is_active", columnList = "is_active"),
    @Index(name = "idx_effective_date", columnList = "effective_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationFee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String specialization;
    
    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;
    
    @Column(length = 3)
    private String currency = "USD";
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_by")
    private Long createdBy; // Admin user ID
    
    @Column(name = "updated_by")
    private Long updatedBy; // Admin user ID
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (effectiveDate == null) {
            effectiveDate = LocalDateTime.now();
        }
        if (currency == null) {
            currency = "USD";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}