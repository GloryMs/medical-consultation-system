package com.supervisorservice.dto;

import com.commonlibrary.entity.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for coupon information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    
    private Long id;
    private String couponCode;
    private Long supervisorId;
    private Long patientId;
    private BigDecimal amount;
    private String currency;
    private Long caseId;
    private CouponStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;
    private Long issuedBy;
    private Long batchId;
    private String batchCode;
    private String notes;
    private String cancellationReason;
    private Boolean isExpiringSoon;
    private Integer daysUntilExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Patient details (optional)
    private String patientFirstName;
    private String patientLastName;
    
    // Supervisor details (optional)
    private String supervisorFullName;
}
