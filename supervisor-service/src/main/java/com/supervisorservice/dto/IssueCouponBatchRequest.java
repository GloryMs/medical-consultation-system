package com.supervisorservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a batch of coupons (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCouponBatchRequest {
    
    @NotNull(message = "Supervisor ID is required")
    private Long supervisorId;
    
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    @NotNull(message = "Total coupons is required")
    @Min(value = 1, message = "Total coupons must be at least 1")
    private Integer totalCoupons;
    
    @NotNull(message = "Amount per coupon is required")
    @DecimalMin(value = "0.01", message = "Amount per coupon must be greater than 0")
    private BigDecimal amountPerCoupon;
    
    @Min(value = 1, message = "Expiry months must be at least 1")
    private Integer expiryMonths; // Default will be 6 if not provided
    
    private String notes;
}
