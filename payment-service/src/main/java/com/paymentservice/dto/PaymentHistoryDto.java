package com.paymentservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentHistoryDto {
    private Long id;
    private String paymentType;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime processedAt;
    private String description;
}