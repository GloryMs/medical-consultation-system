package com.supervisorservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a single coupon (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCouponRequest {
    
    @NotNull(message = "Supervisor ID is required")
    private Long supervisorId;
    
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Min(value = 1, message = "Expiry months must be at least 1")
    private Integer expiryMonths; // Default will be 6 if not provided
    
    private String notes;
}
