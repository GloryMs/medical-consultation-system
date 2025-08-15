package com.adminservice.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DoctorPerformanceReportDto {
    private String doctorName;
    private String specialization;
    private Integer totalConsultations;
    private Integer completedConsultations;
    private Integer cancelledAppointments;
    private Double averageRating;
    private Double totalRevenue;
    private Integer averageConsultationTime;
    private Double patientSatisfactionScore;
    private Double responseTime;
    private Map<String, Integer> casesByCategory;
    private Map<String, Integer> casesByUrgency;
    private List<MonthlyPerformanceDto> monthlyTrend;
}