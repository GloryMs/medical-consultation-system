package com.paymentservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    private Long doctorId;

    private Long caseId;

    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal platformFee;

    private BigDecimal doctorAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String paymentMethod;

    private String transactionId;

    private String gatewayResponse;

    private LocalDateTime processedAt;

    private LocalDateTime refundedAt;

    private String refundReason;

    private String currency = "USD";
}
