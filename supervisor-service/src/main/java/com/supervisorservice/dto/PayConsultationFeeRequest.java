package com.supervisorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for paying consultation fee.
 * Supports multiple payment methods: STRIPE, PAYPAL, COUPON.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayConsultationFeeRequest {

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long doctorId;

    private Long appointmentId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // STRIPE, PAYPAL, COUPON

    private BigDecimal amount;

    // For COUPON payment
    private String couponCode;

    // For STRIPE payment
    private String stripePaymentIntentId;
    private String stripeClientSecret;

    // For PAYPAL payment
    private String paypalOrderId;

    private String notes;
}