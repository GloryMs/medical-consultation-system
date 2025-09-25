package com.doctorservice.dto;

import lombok.Data;

@Data
public class DoctorDetailsDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String licenseNumber;
    private String primarySpecialization;
    private String subSpecialization;
    private Double averageRating;
    private Integer consultationCount;
}