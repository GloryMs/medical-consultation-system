package com.supervisorservice.dto;

import com.commonlibrary.entity.CouponStatus;
import com.commonlibrary.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for supervisor coupon information
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorCouponDto {

    private Long id;

    private String couponCode;

    private Long supervisorId;

    private Long patientId;

    private String patientName;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private CouponStatus status;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    private Long usedForCaseId;

    private Long batchId;

    private String notes;

    /**
     * Days until expiration (for display)
     */
    private Integer daysUntilExpiry;

    /**
     * Whether coupon is expiring soon (within 7 days)
     */
    private boolean expiringSoon;
}