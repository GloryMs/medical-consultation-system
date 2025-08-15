package com.adminservice.dto;

import lombok.Data;

@Data
public class RefundRequestDto {
    private String paymentId;
    private Double refundAmount;
    private String reason;
}