package com.supervisorservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for redeeming a coupon for case payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemCouponRequest {
    
    @NotBlank(message = "Coupon code is required")
    private String couponCode;
}
