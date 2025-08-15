package com.adminservice.dto;

import lombok.Data;

@Data
public class DoctorRevenueDto {
    private Long doctorId;
    private String doctorName;
    private Double revenue;
}