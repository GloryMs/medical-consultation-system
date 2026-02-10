package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTrendMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    // Combined Revenue & Transaction Trend (for Composed Chart)
    private List<CombinedTrendPoint> combinedTrend;

    // Day of Week Analysis (for Line Chart)
    private List<DayOfWeekStats> dayOfWeekTrend;

    // Monthly Growth Rate (for Line Chart)
    private List<MonthlyGrowth> monthlyGrowth;

    // Peak Insights
    private PeakInsight peakDay;
    private PeakInsight bestMonth;
    private String trendDirection;  // "up", "down", or "stable"
    private BigDecimal forecastNextMonth;

    // Seasonal Analysis
    private List<SeasonalAnalysis> seasonalAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedTrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private BigDecimal revenue;
        private Long transactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayOfWeekStats implements Serializable {
        private static final long serialVersionUID = 1L;

        private String day;
        private BigDecimal revenue;
        private Long transactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyGrowth implements Serializable {
        private static final long serialVersionUID = 1L;

        private String month;
        private Double growth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakInsight implements Serializable {
        private static final long serialVersionUID = 1L;

        private String day;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalAnalysis implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private BigDecimal avgRevenue;
        private Long avgTransactions;
        private Double vsAverage;
    }
}