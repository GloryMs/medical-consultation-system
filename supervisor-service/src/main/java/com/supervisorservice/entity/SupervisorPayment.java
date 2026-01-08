package com.supervisorservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track payments made by supervisors on behalf of patients
 * This is a local record; actual payment processing is done by payment-service
 */
@Entity
@Table(name = "supervisor_payments", indexes = {
    @Index(name = "idx_supervisor_id", columnList = "supervisor_id"),
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_case_id", columnList = "case_id"),
    @Index(name = "idx_payment_method", columnList = "payment_method"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supervisor_id", nullable = false)
    private Long supervisorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethodType paymentMethod;

    /**
     * Original consultation fee
     */
    @Column(name = "original_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal originalAmount;

    /**
     * Discount amount (if coupon used)
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Final amount charged
     */
    @Column(name = "final_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SupervisorPaymentStatus status = SupervisorPaymentStatus.PENDING;

    /**
     * Coupon code if payment was via coupon
     */
    @Column(name = "coupon_code")
    private String couponCode;

    /**
     * Coupon ID if payment was via coupon
     */
    @Column(name = "coupon_id")
    private Long couponId;

    /**
     * External payment ID from payment-service
     */
    @Column(name = "external_payment_id")
    private Long externalPaymentId;

    /**
     * Stripe payment intent ID
     */
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    /**
     * Stripe charge ID
     */
    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    /**
     * PayPal order ID
     */
    @Column(name = "paypal_order_id")
    private String paypalOrderId;

    /**
     * Internal transaction ID
     */
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    /**
     * Receipt URL
     */
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enum for payment methods used by supervisor
     */
    public enum PaymentMethodType {
        STRIPE,
        PAYPAL,
        COUPON
    }

    /**
     * Enum for payment status
     */
    public enum SupervisorPaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED,
        CANCELLED
    }
}