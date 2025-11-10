package com.paymentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponseDto {

    private Long refundId;
    private Long paymentId;
    private BigDecimal refundAmount;
    private BigDecimal refundFee;
    private String status;
    private String stripeRefundId;
    private LocalDateTime processedAt;
    private String message;
}
