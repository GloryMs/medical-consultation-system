package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for validating a coupon.
 * Used by payment-service to validate coupons with admin-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationRequest {
    
    /**
     * The coupon code to validate
     */
    private String couponCode;
    
    /**
     * Type of beneficiary requesting validation
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * ID of the beneficiary (supervisor or patient)
     */
    private Long beneficiaryId;
    
    /**
     * Patient ID for whom the coupon will be used
     */
    private Long patientId;
    
    /**
     * Case ID for which the coupon will be applied
     */
    private Long caseId;
    
    /**
     * The consultation fee amount to apply discount against
     */
    private BigDecimal requestedAmount;
}