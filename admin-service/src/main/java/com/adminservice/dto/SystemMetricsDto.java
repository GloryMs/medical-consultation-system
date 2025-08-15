package com.adminservice.dto;

import lombok.Data;

@Data
public class SystemMetricsDto {
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisMonth;
    private Long totalCases;
    private Long casesInProgress;
    private Double averageCaseResolutionTime;
    private Double totalRevenue;
    private Double revenueThisMonth;
    private Double averageConsultationFee;
    private Double systemUptime;
    private Integer activeServices;
    private Double errorRate;
}
