package com.adminservice.dto;

import lombok.Data;

@Data
public class PatientSpendingDto {
    private Long patientId;
    private String patientName;
    private Double totalSpending;
}