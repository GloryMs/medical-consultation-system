package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Performance metrics for case workflow analysis
 * Focuses on time-based performance and bottlenecks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePerformanceMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Average time spent in each status (hours)
     * Key: Status name, Value: Average hours
     */
    private Map<String, Double> avgTimeByStatus;
    
    /**
     * Bottleneck analysis - cases stuck in each status
     * Key: Status name, Value: Number of stuck cases
     */
    private Map<String, Long> bottleneckAnalysis;
    
    /**
     * SLA compliance metrics by urgency level
     */
    private SlaComplianceDto slaCompliance;
    
    /**
     * Case stage funnel - shows dropoff at each stage
     */
    private List<CaseStageMetrics> stageFunnel;
    
    /**
     * Performance by urgency level
     */
    private Map<String, UrgencyPerformance> performanceByUrgency;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UrgencyPerformance implements Serializable {
        private String urgencyLevel;
        private Long totalCases;
        private Double avgAssignmentTime; // hours
        private Double avgResolutionTime; // days
        private Double slaCompliance; // percentage
    }
}