package com.paymentservice.entity;

import com.commonlibrary.converter.PaymentMethodConverter;
import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.PaymentMethod;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Payment entity with Stripe integration fields
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_patient_id", columnList = "patient_id"),
        @Index(name = "idx_doctor_id", columnList = "doctor_id"),
        @Index(name = "idx_case_id", columnList = "case_id"),
        @Index(name = "idx_stripe_payment_intent", columnList = "stripe_payment_intent_id"),
        @Index(name = "idx_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {


    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private Long caseId;

    private Long supervisorId;

    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentType paymentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(precision = 10, scale = 2)
    private BigDecimal doctorAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Convert(converter = PaymentMethodConverter.class)
    @Column(length = 50)
    private PaymentMethod paymentMethod;

    @Column(unique = true)
    private String transactionId;

    // Stripe specific fields
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_payment_method_id")
    private String stripePaymentMethodId;

    @Column(name = "stripe_refund_id")
    private String stripeRefundId;

    @Column(name = "stripe_transfer_id")
    private String stripeTransferId;

    @Column(name = "stripe_fee", precision = 10, scale = 2)
    private BigDecimal stripeFee;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "payment_link", length = 500)
    private String paymentLink;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;

    private LocalDateTime processedAt;

    private LocalDateTime refundedAt;

    @Column(columnDefinition = "TEXT")
    private String refundReason;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundFee;

    @Column(length = 3)
    private String currency = "USD";

    // Idempotency key for preventing duplicate payments
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    // Metadata for additional information
    @ElementCollection
    @CollectionTable(name = "payment_metadata", joinColumns = @JoinColumn(name = "payment_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();

    // Audit fields
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // Helper methods
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    protected void onCreate() {
        super.onCreate(); // Call superclass logic
        if (this.currency == null) {
            this.currency = "USD";
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }
}