package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Subscription entity for managing patient and doctor subscriptions
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_user_id_type", columnList = "user_id,user_type"),
    @Index(name = "idx_stripe_subscription_id", columnList = "stripe_subscription_id", unique = true),
    @Index(name = "idx_stripe_customer_id", columnList = "stripe_customer_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_current_period_end", columnList = "current_period_end")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "user_type", nullable = false, length = 20)
    private String userType; // PATIENT or DOCTOR
    
    @Column(name = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;
    
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;
    
    @Column(name = "plan_type", length = 50)
    private String planType; // BASIC, PREMIUM, PRO
    
    @Column(name = "plan_duration")
    private Integer planDuration; // in months
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 3)
    private String currency = "USD";
    
    @Column(length = 50)
    private String status; // active, trialing, past_due, canceled, unpaid
    
    @Column(name = "trial_start")
    private LocalDateTime trialStart;
    
    @Column(name = "trial_end")
    private LocalDateTime trialEnd;
    
    @Column(name = "trial_period_days")
    private Integer trialPeriodDays;
    
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;
    
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;
    
    @Column(name = "cancel_at_period_end")
    private boolean cancelAtPeriodEnd = false;
    
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    // Stripe price ID for the subscription
    @Column(name = "stripe_price_id")
    private String stripePriceId;
    
    // Payment method used for subscription
    @Column(name = "stripe_payment_method_id")
    private String stripePaymentMethodId;
    
    // Metadata for additional information
    @ElementCollection
    @CollectionTable(name = "subscription_metadata", joinColumns = @JoinColumn(name = "subscription_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();
    
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
    
    // Helper methods
    public boolean isActive() {
        return "active".equals(status) || "trialing".equals(status);
    }
    
    public boolean isTrialing() {
        return "trialing".equals(status);
    }
    
    public boolean isCanceled() {
        return "canceled".equals(status);
    }
    
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}