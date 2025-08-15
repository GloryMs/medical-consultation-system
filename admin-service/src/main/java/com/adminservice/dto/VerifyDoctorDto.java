package com.adminservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyDoctorDto {
    @NotNull
    private Long doctorId;

    @NotNull
    private Boolean approved;

    private String reason;
}
