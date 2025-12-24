package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SLA (Service Level Agreement) compliance metrics
 * Tracks how well cases meet urgency-based time targets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaComplianceDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * SLA targets in hours
     */
    private Integer criticalTarget = 1;  // CRITICAL cases should be assigned within 1 hour
    private Integer highTarget = 4;      // HIGH cases within 4 hours
    private Integer mediumTarget = 24;   // MEDIUM cases within 24 hours
    private Integer lowTarget = 48;      // LOW cases within 48 hours
    
    /**
     * Compliance percentages for each urgency level
     */
    private Double criticalCompliance;  // % of CRITICAL cases meeting 1hr target
    private Double highCompliance;      // % of HIGH cases meeting 4hr target
    private Double mediumCompliance;    // % of MEDIUM cases meeting 24hr target
    private Double lowCompliance;       // % of LOW cases meeting 48hr target
    
    /**
     * Overall compliance across all urgency levels
     */
    private Double overallCompliance;
    
    /**
     * Count of cases by compliance status
     */
    private Long totalCases;
    private Long casesMetSla;
    private Long casesMissedSla;
    
    /**
     * Average time to assignment by urgency (hours)
     */
    private Double criticalAvgTime;
    private Double highAvgTime;
    private Double mediumAvgTime;
    private Double lowAvgTime;
}