package com.patientservice.dto;

import com.patientservice.entity.UrgencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateCaseDto {
    @NotBlank(message = "Case title is required")
    private String caseTitle;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String subCategory;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgencyLevel;

    private List<Long> documentIds;
}