package com.patientservice.dto;

import com.patientservice.entity.ComplaintPriority;
import com.patientservice.entity.ComplaintType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplaintDto {
    private Long doctorId;
    private Long caseId;

    @NotNull(message = "Complaint type is required")
    private ComplaintType complaintType;

    @NotBlank(message = "Description is required")
    private String description;

    private ComplaintPriority priority = ComplaintPriority.MEDIUM;
}
