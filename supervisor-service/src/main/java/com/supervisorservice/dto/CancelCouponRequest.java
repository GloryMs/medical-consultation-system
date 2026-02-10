package com.supervisorservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for cancelling a coupon (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelCouponRequest {
    
    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
