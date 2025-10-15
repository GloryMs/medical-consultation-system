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
 * DTO for returning dependent information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependentDto {
    
    private Long id;

    private String fullName;

    private String relationship;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String medicalHistory;

    private String bloodGroup;

    private String allergies;

    private String chronicConditions;

    private String phoneNumber;

    private String profilePicture;

    private Integer age; // Calculated field

    // Count of cases submitted for this dependent
    private Integer casesCount;
}