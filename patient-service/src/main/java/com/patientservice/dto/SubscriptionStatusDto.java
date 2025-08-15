package com.patientservice.dto;

import com.patientservice.entity.PlanType;
import com.patientservice.entity.SubscriptionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubscriptionStatusDto {
    private SubscriptionStatus status;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private PlanType planType;
    private BigDecimal amount;
    private Boolean autoRenew;
    private String paymentMethod;
}
