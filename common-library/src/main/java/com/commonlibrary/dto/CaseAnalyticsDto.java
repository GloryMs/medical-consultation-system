package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Main container for comprehensive case analytics
 * Contains all analytics sections for the admin case management dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAnalyticsDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Overview metrics - key performance indicators
     */
    private CaseOverviewMetrics overview;
    
    /**
     * Performance metrics - time-based analysis
     */
    private CasePerformanceMetrics performance;
    
    /**
     * Doctor-specific analytics
     */
    private DoctorAnalyticsMetrics doctorMetrics;
    
    /**
     * Specialization-specific analytics
     */
    private SpecializationAnalyticsMetrics specializationMetrics;
    
    /**
     * Trend analytics - time series data
     */
    private TrendAnalyticsMetrics trends;
    
    /**
     * Quality metrics - case quality indicators
     */
    private QualityMetricsDto qualityMetrics;
    
    /**
     * Date range for the analytics
     */
    private String startDate;
    private String endDate;
    
    /**
     * Metadata
     */
    private Long totalCasesAnalyzed;
    private String generatedAt;
}