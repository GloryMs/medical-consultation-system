package com.commonlibrary.dto;

import com.commonlibrary.entity.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProcessPaymentDto {
    @NotNull
    private Long patientId;

    private Long doctorId;

    private Long caseId;

    private Long supervisorId;

    private Long appointmentId;

    @NotNull
    private PaymentType paymentType;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String paymentMethod;

    private String couponCode;
    private String stripePaymentIntentId;
    private String notes;
}
