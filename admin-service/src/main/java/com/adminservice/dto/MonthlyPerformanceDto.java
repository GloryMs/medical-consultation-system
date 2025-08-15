package com.adminservice.dto;

import lombok.Data;

@Data
public class MonthlyPerformanceDto {
    private String month;
    private Integer consultations;
    private Double revenue;
    private Double rating;
}