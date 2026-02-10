package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRevenueMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    // Revenue Trend Over Time (for Area/Line Chart)
    private List<RevenueTrendPoint> revenueTrend;

    // Revenue by Payment Type (for Bar Chart)
    private Map<String, BigDecimal> revenueByType;

    // Monthly Comparison (for Line Chart)
    private List<MonthlyRevenueComparison> monthlyComparison;

    // Key Metrics
    private HighestRevenueDay highestRevenueDay;
    private BigDecimal avgDailyRevenue;
    private Double growthRate;
    private BigDecimal revenuePerTransaction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueTrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueComparison implements Serializable {
        private static final long serialVersionUID = 1L;

        private String month;
        private BigDecimal currentYear;
        private BigDecimal previousYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighestRevenueDay implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private BigDecimal amount;
    }
}