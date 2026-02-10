package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOverviewMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    // KPI Metrics
    private BigDecimal totalRevenue;
    private Long totalPayments;
    private Long completedPayments;
    private Long failedPayments;
    private Long pendingPayments;
    private Long totalRefunds;
    private Double successRate;

    // Financial Metrics
    private BigDecimal avgTransactionValue;
    private BigDecimal totalRefundedAmount;
    private Double refundRate;

    // Distribution Data
    private Map<String, Long> statusDistribution;
    private Map<String, Long> typeDistribution;

    // Trend Indicators (optional - for showing growth indicators)
    private TrendIndicator revenueTrend;
    private TrendIndicator paymentTrend;
    private TrendIndicator avgTransactionTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendIndicator implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean isPositive;
        private Double value;
        private String period;
    }
}