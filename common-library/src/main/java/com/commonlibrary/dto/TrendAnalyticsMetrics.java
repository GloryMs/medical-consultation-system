package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Time-based trend analytics
 * Patterns, seasonality, and temporal insights
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalyticsMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Daily trend (last 30 days)
     * Shows case volume per day
     */
    private List<ChartDataPointDto> dailyTrend;
    
    /**
     * Weekly trend (last 12 weeks)
     * Shows case volume per week
     */
    private List<ChartDataPointDto> weeklyTrend;
    
    /**
     * Monthly trend (last 12 months)
     * Shows case volume per month
     */
    private List<ChartDataPointDto> monthlyTrend;
    
    /**
     * Hourly distribution (0-23 hours)
     * Key: Hour (0-23), Value: Case count
     */
    private Map<Integer, Integer> hourlyDistribution;
    
    /**
     * Day of week distribution
     * Key: Day name, Value: Case count
     */
    private Map<String, Integer> dayOfWeekDistribution;
    
    /**
     * Status trend over time
     * Track how status distribution changes
     */
    private List<StatusTrendPoint> statusTrend;
    
    /**
     * Peak activity insights
     */
    private Integer peakHour;              // Hour with most submissions (0-23)
    private String peakDay;                // Day with most submissions
    private String peakMonth;              // Month with most submissions
    
    /**
     * Growth metrics
     */
    private Double weekOverWeekGrowth;     // % change from last week
    private Double monthOverMonthGrowth;   // % change from last month
    private Double yearOverYearGrowth;     // % change from last year
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusTrendPoint implements Serializable {
        private String date;
        private String status;
        private Long count;
    }
}