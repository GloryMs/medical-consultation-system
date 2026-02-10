package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for coupon summary statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSummaryDto {
    
    private Long supervisorId;
    
    // Counts
    private Integer totalCoupons;
    private Integer availableCoupons;
    private Integer usedCoupons;
    private Integer expiredCoupons;
    private Integer cancelledCoupons;
    private Integer couponsExpiringSoon;
    
    // Values
    private BigDecimal totalAvailableValue;
    private BigDecimal totalUsedValue;
    private BigDecimal totalExpiredValue;
    
    // Patient breakdown
    private List<PatientCouponSummary> patientSummaries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientCouponSummary {
        private Long patientId;
        private String patientName;
        private Integer availableCoupons;
        private BigDecimal availableValue;
        private Integer usedCoupons;
        private Integer expiredCoupons;
    }
}
