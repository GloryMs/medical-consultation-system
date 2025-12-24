package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Specialization-specific analytics
 * Volume, performance, and trend analysis by medical specialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecializationAnalyticsMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Case volume by specialization
     * Key: Specialization name, Value: Case count
     */
    private Map<String, Long> casesBySpecialization;
    
    /**
     * Average resolution time by specialization (days)
     * Key: Specialization name, Value: Avg days
     */
    private Map<String, Double> avgResolutionBySpecialization;
    
    /**
     * Average consultation fee by specialization
     * Key: Specialization name, Value: Avg fee
     */
    private Map<String, BigDecimal> avgFeeBySpecialization;
    
    /**
     * Specialization trends (growth/decline)
     */
    private List<SpecializationTrendDto> specializationTrends;
    
    /**
     * Top insights
     */
    private String mostInDemand;          // Specialization with most cases
    private String fastestResolution;     // Specialization with fastest resolution
    private String highestFee;            // Specialization with highest avg fee
    private String fastestGrowth;         // Specialization growing fastest
    
    /**
     * Distribution percentage
     * Key: Specialization name, Value: Percentage of total
     */
    private Map<String, Double> distributionPercentage;
    
    /**
     * Total unique specializations
     */
    private Integer totalSpecializations;
}