package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Subscription Plan entity for managing available subscription plans
 */
@Entity
@Table(name = "subscription_plans", indexes = {
    @Index(name = "idx_stripe_product_id", columnList = "stripe_product_id"),
    @Index(name = "idx_stripe_price_id", columnList = "stripe_price_id"),
    @Index(name = "idx_plan_type_user_type", columnList = "plan_type,user_type"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "stripe_product_id")
    private String stripeProductId;
    
    @Column(name = "stripe_price_id")
    private String stripePriceId;
    
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;
    
    @Column(name = "plan_type", length = 50)
    private String planType; // BASIC, PREMIUM, PRO
    
    @Column(name = "user_type", length = 20)
    private String userType; // PATIENT, DOCTOR
    
    @Column(name = "duration_months")
    private Integer durationMonths; // 6, 12 for patients; 12 for doctors
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 3)
    private String currency = "USD";
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Features as JSON
    @ElementCollection
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    // Trial period for this plan (primarily for doctors)
    @Column(name = "trial_period_days")
    private Integer trialPeriodDays;
    
    // Limits and quotas
    @Column(name = "max_consultations_per_month")
    private Integer maxConsultationsPerMonth;
    
    @Column(name = "max_file_uploads_per_case")
    private Integer maxFileUploadsPerCase;
    
    @Column(name = "priority_support")
    private boolean prioritySupport = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currency == null) {
            currency = "USD";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}