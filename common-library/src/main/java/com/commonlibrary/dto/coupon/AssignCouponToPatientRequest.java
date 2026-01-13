package com.commonlibrary.dto.coupon;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for supervisor to assign a coupon to a patient.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCouponToPatientRequest {
    
    /**
     * Patient ID to assign the coupon to
     */
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    /**
     * Notes about this assignment
     */
    private String notes;
}