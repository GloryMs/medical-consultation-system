package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for distributing a coupon to a beneficiary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributeCouponRequest {
    
    /**
     * Type of beneficiary to distribute to
     */
    @NotNull(message = "Beneficiary type is required")
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (supervisor or patient)
     */
    @NotNull(message = "Beneficiary ID is required")
    private Long beneficiaryId;
    
    /**
     * Notes about this distribution
     */
    private String notes;
    
    /**
     * Whether to send notification to beneficiary
     */
    @Builder.Default
    private Boolean sendNotification = true;
}