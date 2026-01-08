package com.supervisorservice.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for Stripe payment intent response
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentDto {

    private Long paymentId;

    private String paymentIntentId;

    private String clientSecret;

    private BigDecimal amount;

    private String currency;

    private String status;

    private Long caseId;

    private Long patientId;

    private Long doctorId;
}