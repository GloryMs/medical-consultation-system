package com.commonlibrary.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for cancelling a coupon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelCouponRequest {
    
    /**
     * Reason for cancellation
     */
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
    
    /**
     * Whether to send notification to beneficiary
     */
    @Builder.Default
    private Boolean sendNotification = true;
}