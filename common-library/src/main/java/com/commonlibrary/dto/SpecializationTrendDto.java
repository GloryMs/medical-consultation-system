package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Trend data for individual specialization
 * Shows growth or decline over time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecializationTrendDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Specialization name
     */
    private String specialization;
    
    /**
     * Current period metrics
     */
    private Long currentMonthCases;
    private Long currentWeekCases;
    
    /**
     * Previous period metrics (for comparison)
     */
    private Long previousMonthCases;
    private Long previousWeekCases;
    
    /**
     * Growth rate calculations
     */
    private Double monthlyGrowthRate;   // % change month over month
    private Double weeklyGrowthRate;    // % change week over week
    
    /**
     * Trend indicators
     */
    private String trendDirection;      // "UP", "DOWN", "STABLE"
    private Boolean isGrowing;
    
    /**
     * Absolute changes
     */
    private Long monthlyChange;         // Absolute difference
    private Long weeklyChange;          // Absolute difference
}