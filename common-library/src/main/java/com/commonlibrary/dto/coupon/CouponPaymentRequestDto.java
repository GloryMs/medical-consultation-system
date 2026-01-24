package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for processing coupon-based payments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPaymentRequestDto {

    @NotBlank(message = "Coupon code is required")
    private String couponCode;

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long doctorId;

    private Long appointmentId;

    private Long supervisorId;

    /**
     * Beneficiary type - who owns the coupon (MEDICAL_SUPERVISOR or PATIENT)
     */
    @NotNull(message = "Beneficiary type is required")
    private BeneficiaryType beneficiaryType;

    /**
     * Beneficiary ID - supervisor ID or patient ID depending on type
     */
    @NotNull(message = "Beneficiary ID is required")
    private Long beneficiaryId;

    /**
     * Consultation fee to be paid. If null, default fee is used.
     */
    private BigDecimal consultationFee;

    /**
     * User ID of the person redeeming the coupon
     */
    private Long redeemedByUserId;

    /**
     * Optional notes about the payment
     */
    private String notes;
}