package com.paymentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundRequestDto {
    private Long paymentId;
    private BigDecimal amount; // null for full refund
    private String reason;
    private String refundType; // DOCTOR_NO_SHOW, INCOMPLETE_CONSULTATION, etc.
    private Long initiatedBy;
    private String initiatorRole; // ADMIN, SYSTEM, PATIENT

}
