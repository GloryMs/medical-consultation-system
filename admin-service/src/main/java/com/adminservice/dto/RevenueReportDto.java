package com.adminservice.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RevenueReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalRevenue;
    private Double subscriptionRevenue;
    private Double consultationRevenue;
    private Double platformFees;
    private Double doctorPayouts;
    private Double refunds;
    private Double netRevenue;
    private List<DailyRevenueDto> dailyBreakdown;
    private List<DoctorRevenueDto> topDoctorsByRevenue;
    private List<PatientSpendingDto> topPatientsBySpending;
}