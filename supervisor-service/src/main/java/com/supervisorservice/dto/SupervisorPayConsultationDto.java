package com.supervisorservice.dto;

import com.commonlibrary.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for supervisor paying consultation fee on behalf of patient
 * Supports multiple payment methods: STRIPE, PAYPAL, COUPON
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorPayConsultationDto {

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    private Long appointmentId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    /**
     * Required when paymentMethod = COUPON
     */
    private String couponCode;

    /**
     * Stripe payment method ID (for saved cards)
     */
    private String paymentMethodId;

    /**
     * Stripe payment intent ID (from frontend)
     */
    private String paymentIntentId;

    /**
     * PayPal order ID
     */
    private String paypalOrderId;

    /**
     * Optional notes for the payment
     */
    private String notes;
}