package com.adminservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentRecordDto {
    private Long id;
    private String type;
    private Double amount;
    private String status;
    private LocalDateTime processedAt;
}