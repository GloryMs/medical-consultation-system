package com.doctorservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SetCaseFeeDto {
    
    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "100.00", message = "Consultation fee must be at least $100.00")
    @DecimalMax(value = "500.00", message = "Consultation fee cannot exceed $500.00")
    private BigDecimal consultationFee;
}