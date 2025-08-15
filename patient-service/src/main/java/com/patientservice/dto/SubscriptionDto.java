package com.patientservice.dto;

import com.patientservice.entity.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubscriptionDto {
    private Long id;

    @NotNull(message = "Plan type is required")
    private PlanType planType;

    private BigDecimal amount;
    private String paymentMethod;
    private Boolean autoRenew;
}