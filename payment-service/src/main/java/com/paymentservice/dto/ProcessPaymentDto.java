package com.paymentservice.dto;

import com.paymentservice.entity.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessPaymentDto {
    @NotNull
    private Long patientId;

    private Long doctorId;

    private Long caseId;

    @NotNull
    private PaymentType paymentType;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String paymentMethod;
}
