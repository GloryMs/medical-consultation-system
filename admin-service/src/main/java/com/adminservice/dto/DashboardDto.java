package com.adminservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardDto {
    private Long totalUsers;
    private Long totalDoctors;
    private Long totalPatients;
    private Long activeCases;
    private Long completedCases;
    private Long pendingVerifications;
    private Double totalRevenue;
    private Long activeSubscriptions;
}
