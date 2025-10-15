package com.patientservice.dto;

import com.commonlibrary.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new dependent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDependentDto {
    
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Relationship is required")
    private String relationship; // SON, DAUGHTER, WIFE, HUSBAND, MOTHER, FATHER, OTHER

    private LocalDate dateOfBirth;

    private Gender gender;

    private String medicalHistory;

    private String bloodGroup;

    private String allergies;

    private String chronicConditions;

    private String phoneNumber;
}



