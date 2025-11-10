package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RefundLog entity for tracking all refund transactions
 */
@Entity
@Table(name = "refund_logs", indexes = {
        @Index(name = "idx_payment_id", columnList = "payment_id"),
        @Index(name = "idx_stripe_refund_id", columnList = "stripe_refund_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_processed_at", columnList = "processed_at"),
        @Index(name = "idx_initiated_by", columnList = "initiated_by")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "stripe_refund_id")
    private String stripeRefundId;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_fee", precision = 10, scale = 2)
    private BigDecimal refundFee; // Stripe fees that won't be refunded

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refund_type", length = 50)
    private String refundType; // DOCTOR_NO_SHOW, INCOMPLETE_CONSULTATION, PARTIAL_REFUND, etc.

    @Column(length = 50)
    private String status; // PENDING, COMPLETED, FAILED

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy; // User ID who initiated the refund

    @Column(name = "initiator_role", length = 20)
    private String initiatorRole; // ADMIN, SYSTEM, PATIENT

    @Column(name = "approved_by")
    private Long approvedBy; // Admin who approved the refund

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}