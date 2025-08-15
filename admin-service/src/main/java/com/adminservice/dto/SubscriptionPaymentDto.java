package com.adminservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionPaymentDto {
    private Long patientId;
    private String planType;
    private Double amount;
    private String status;
    private LocalDateTime paymentDate;
}