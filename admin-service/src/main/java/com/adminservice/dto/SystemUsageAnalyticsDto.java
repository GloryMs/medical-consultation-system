package com.adminservice.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SystemUsageAnalyticsDto {
    private String period;
    private Map<String, Integer> dailyActiveUsers;
    private Integer monthlyActiveUsers;
    private Map<Integer, Integer> peakUsageHours;
    private Map<String, Integer> featureUsage;
    private List<String> mostUsedFeatures;
    private Double caseSubmissionRate;
    private Double averageTimeToAssignment;
    private Map<String, Integer> specializationDemand;
    private Double subscriptionConversionRate;
    private Double paymentSuccessRate;
    private Double averageRevenuePerUser;
    private Double apiResponseTime;
    private Double errorRate;
    private Double systemAvailability;
}