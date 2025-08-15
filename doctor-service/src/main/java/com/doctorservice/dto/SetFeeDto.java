package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SetFeeDto {
    @NotNull(message = "Consultation fee is required")
    private BigDecimal consultationFee;

    private String reason;
}
