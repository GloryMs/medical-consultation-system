package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Quality metrics for case management
 * Measures efficiency, accuracy, and completeness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityMetricsDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Reassignment rate - % of cases reassigned to different doctor
     * Lower is better (indicates good initial matching)
     */
    private Double reassignmentRate;
    private Long totalReassignments;
    
    /**
     * Rejection rate - % of cases rejected by assigned doctors
     * Lower is better (indicates good doctor matching)
     */
    private Double rejectionRate;
    private Long totalRejections;
    
    /**
     * Completion rate - % of cases that reach CLOSED status
     * Higher is better
     */
    private Double completionRate;
    private Long completedCases;
    private Long abandonedCases;
    
    /**
     * Documentation quality score (%)
     * Based on completeness of case information
     */
    private Double avgDocumentationScore;
    private Long casesWithCompleteInfo;
    private Long casesWithIncompleteInfo;
    
    /**
     * Iteration metrics
     * How many back-and-forth interactions before resolution
     */
    private Double avgIterationsPerCase;
    private Integer maxIterations;
    private Integer minIterations;
    
    /**
     * First-time assignment success rate
     * % of cases accepted by first assigned doctor
     */
    private Double firstTimeSuccessRate;
    
    /**
     * Average time to first response (hours)
     * How quickly doctors respond to assignments
     */
    private Double avgTimeToFirstResponse;
    
    /**
     * Cases requiring attention
     */
    private Long casesWithMultipleReassignments;  // Reassigned 2+ times
    private Long casesWithLongResponseTime;       // Response time > 24hrs
    private Long casesWithIncompleteDocumentation;
    
    /**
     * Quality trend
     */
    private QualityTrend qualityTrend;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityTrend implements Serializable {
        private String direction;  // "IMPROVING", "DECLINING", "STABLE"
        private Double changePercentage;
        private String period;     // "vs last month"
    }
}