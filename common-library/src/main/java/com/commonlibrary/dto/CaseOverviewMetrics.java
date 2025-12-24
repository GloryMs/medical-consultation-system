package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Overview metrics for case management dashboard
 * Key performance indicators at a glance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseOverviewMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Total cases in the system
     */
    private Long totalCases;
    
    /**
     * Active cases (not closed or rejected)
     */
    private Long activeCases;
    
    /**
     * Closed cases
     */
    private Long closedCases;
    
    /**
     * Cases at risk (overdue based on urgency SLA)
     */
    private Long casesAtRisk;
    
    /**
     * Assignment success rate (%)
     */
    private Double assignmentSuccessRate;
    
    /**
     * Number of active doctors handling cases
     */
    private Integer activeDoctorsCount;
    
    /**
     * Average time from submission to assignment (hours)
     */
    private Double avgAssignmentTime;
    
    /**
     * Average time from submission to closure (days)
     */
    private Double avgResolutionTime;
    
    /**
     * Average time for doctor to respond to assignment (hours)
     */
    private Double avgResponseTime;
    
    /**
     * Distribution by status
     * Key: Status name, Value: Count
     */
    private Map<String, Long> statusDistribution;
    
    /**
     * Distribution by urgency level
     * Key: Urgency level, Value: Count
     */
    private Map<String, Long> urgencyDistribution;
    
    /**
     * Trend indicators
     */
    private TrendIndicator caseTrend;
    private TrendIndicator assignmentTimeTrend;
    private TrendIndicator resolutionTimeTrend;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendIndicator implements Serializable {
        private Double value;
        private Boolean isPositive;
        private String period; // "vs last week", "vs last month"
    }
}