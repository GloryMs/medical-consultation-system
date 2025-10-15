package com.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_codes", indexes = {
    @Index(name = "idx_password_reset_identifier", columnList = "identifier"),
    @Index(name = "idx_password_reset_code", columnList = "code"),
    @Index(name = "idx_password_reset_expiry", columnList = "expiry_time"),
    @Index(name = "idx_password_reset_is_used", columnList = "is_used"),
    @Index(name = "idx_password_reset_lookup", columnList = "identifier,code,is_used,expiry_time")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String identifier; // Email or phone number
    
    @Column(nullable = false, length = 6)
    private String code; // 6-digit verification code
    
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
    
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;
    
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
    
    @Column(name = "delivery_method", nullable = false, length = 20)
    private String deliveryMethod; // EMAIL, SMS, or WHATSAPP
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Check if the code is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryTime);
    }
    
    /**
     * Check if the code is still valid (not used and not expired)
     */
    public boolean isValid() {
        return !this.isUsed && !isExpired();
    }
    
    /**
     * Increment the attempt count
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
    }
    
    /**
     * Check if maximum attempts reached
     */
    public boolean isMaxAttemptsReached(int maxAttempts) {
        return this.attemptCount >= maxAttempts;
    }
    
    /**
     * Mark the code as used
     */
    public void markAsUsed() {
        this.isUsed = true;
    }
}