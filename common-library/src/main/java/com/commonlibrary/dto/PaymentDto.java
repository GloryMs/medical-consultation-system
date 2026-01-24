package com.commonlibrary.dto;

import com.commonlibrary.entity.PaymentMethod;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long paymentId;
    private Long patientId;
    private Long doctorId;
    private Long caseId;
    private Long appointmentId;
    private PaymentType paymentType;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal doctorAmount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private String gatewayResponse;
    private LocalDateTime processedAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private String currency = "USD";
}
