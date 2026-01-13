package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.DiscountType;
import com.commonlibrary.entity.SupervisorCouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a coupon allocation in supervisor-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorCouponAllocationDto {
    
    /**
     * Local allocation ID
     */
    private Long id;
    
    /**
     * Reference to admin-service coupon ID
     */
    private Long adminCouponId;
    
    /**
     * Coupon code
     */
    private String couponCode;
    
    /**
     * Supervisor ID who owns this allocation
     */
    private Long supervisorId;
    
    /**
     * Patient ID this coupon is assigned to (null if unassigned)
     */
    private Long assignedPatientId;
    
    /**
     * Patient name (for display)
     */
    private String assignedPatientName;
    
    /**
     * When the coupon was assigned to patient
     */
    private LocalDateTime assignedAt;
    
    /**
     * Type of discount
     */
    private DiscountType discountType;
    
    /**
     * Discount value
     */
    private BigDecimal discountValue;
    
    /**
     * Maximum discount amount
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Local status
     */
    private SupervisorCouponStatus status;
    
    /**
     * When the coupon was used (if used)
     */
    private LocalDateTime usedAt;
    
    /**
     * Case ID the coupon was used for (if used)
     */
    private Long usedForCaseId;
    
    /**
     * Payment ID the coupon was used for (if used)
     */
    private Long usedForPaymentId;
    
    /**
     * When the coupon expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Whether the coupon is expiring soon (within 30 days)
     */
    private Boolean isExpiringSoon;
    
    /**
     * Days until expiry
     */
    private Integer daysUntilExpiry;
    
    /**
     * When the coupon was received from admin
     */
    private LocalDateTime receivedAt;
    
    /**
     * Last sync time with admin-service
     */
    private LocalDateTime lastSyncedAt;
    
    /**
     * When the record was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the record was last updated
     */
    private LocalDateTime updatedAt;
}