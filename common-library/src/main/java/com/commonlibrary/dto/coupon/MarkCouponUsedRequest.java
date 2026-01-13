package com.commonlibrary.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for marking a coupon as used.
 * Used by payment-service to update coupon status in admin-service after successful payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkCouponUsedRequest {
    
    /**
     * The coupon code to mark as used
     */
    private String couponCode;
    
    /**
     * Case ID the coupon was used for
     */
    private Long caseId;
    
    /**
     * Patient ID the coupon was used for
     */
    private Long patientId;
    
    /**
     * Payment ID created for this transaction
     */
    private Long paymentId;
    
    /**
     * The discount amount that was applied
     */
    private BigDecimal discountApplied;
    
    /**
     * The remaining amount charged (via Stripe if any)
     */
    private BigDecimal amountCharged;
    
    /**
     * When the coupon was used
     */
    private LocalDateTime usedAt;
    
    /**
     * ID of the user who redeemed the coupon (supervisor or patient user ID)
     */
    private Long redeemedByUserId;
}