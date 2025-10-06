package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExportReportDto {
    @NotNull
    private Long reportId;
    private Boolean includePatientInfo = true;
    private Boolean includeDoctorInfo = true;
}