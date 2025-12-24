package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Metrics for individual case workflow stage
 * Used in funnel analysis to show progression and dropoffs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseStageMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Stage name (e.g., SUBMITTED, PENDING, ASSIGNED, etc.)
     */
    private String stageName;
    
    /**
     * Display name for UI
     */
    private String stageLabel;
    
    /**
     * Number of cases in this stage
     */
    private Long caseCount;
    
    /**
     * Number of cases that dropped off at this stage
     * (didn't progress to next stage)
     */
    private Long dropoffCount;
    
    /**
     * Dropoff rate as percentage
     */
    private Double dropoffRate;
    
    /**
     * Average duration cases spend in this stage (hours)
     */
    private Double avgDuration;
    
    /**
     * Percentage of total cases that reach this stage
     */
    private Double reachRate;
    
    /**
     * Stage order (for funnel visualization)
     */
    private Integer stageOrder;
}