package com.patientservice.dto;

import com.commonlibrary.entity.PlanType;
import com.commonlibrary.entity.SubscriptionStatus;
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
