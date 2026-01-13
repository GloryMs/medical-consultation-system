package com.supervisorservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for coupon redemption (legacy endpoint).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemptionRequest {

    @NotBlank(message = "Coupon code is required")
    private String couponCode;
}