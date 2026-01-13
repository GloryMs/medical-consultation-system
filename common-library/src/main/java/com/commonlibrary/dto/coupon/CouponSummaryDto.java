package com.commonlibrary.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for coupon summary and analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSummaryDto {
    
    /**
     * Total number of coupons
     */
    private Integer totalCoupons;
    
    /**
     * Number of coupons created but not distributed
     */
    private Integer createdCoupons;
    
    /**
     * Number of distributed (available) coupons
     */
    private Integer distributedCoupons;
    
    /**
     * Number of used coupons
     */
    private Integer usedCoupons;
    
    /**
     * Number of expired coupons
     */
    private Integer expiredCoupons;
    
    /**
     * Number of cancelled coupons
     */
    private Integer cancelledCoupons;
    
    /**
     * Number of coupons expiring soon (within 30 days)
     */
    private Integer expiringSoonCoupons;
    
    /**
     * Total value of all available coupons
     */
    private BigDecimal totalAvailableValue;
    
    /**
     * Total value redeemed
     */
    private BigDecimal totalRedeemedValue;
    
    /**
     * Total value expired (lost)
     */
    private BigDecimal totalExpiredValue;
    
    /**
     * Summary by beneficiary (for admin view)
     */
    private List<BeneficiaryCouponSummary> beneficiarySummaries;
    
    /**
     * Summary by patient (for supervisor view)
     */
    private List<PatientCouponSummary> patientSummaries;
    
    /**
     * Nested class for beneficiary summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficiaryCouponSummary {
        private String beneficiaryType;
        private Long beneficiaryId;
        private String beneficiaryName;
        private Integer totalCoupons;
        private Integer availableCoupons;
        private Integer usedCoupons;
        private Integer expiredCoupons;
        private BigDecimal availableValue;
        private BigDecimal usedValue;
    }
    
    /**
     * Nested class for patient summary (supervisor view)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientCouponSummary {
        private Long patientId;
        private String patientName;
        private Integer availableCoupons;
        private Integer usedCoupons;
        private Integer expiredCoupons;
        private BigDecimal availableValue;
        private BigDecimal usedValue;
    }
}