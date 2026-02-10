package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    // Refund Metrics
    private Long totalRefunds;
    private BigDecimal totalRefundAmount;
    private Double refundRate;
    private BigDecimal avgRefundAmount;

    // Refund Trend Over Time (for Line Chart with dual Y-axis)
    private List<RefundTrendPoint> refundTrend;

    // Refund Reasons (for Pie Chart) - from RefundLog.refundType
    private List<RefundReasonStats> refundReasons;

    // Refunds by Payment Type (for Bar Chart)
    private Map<String, Long> refundsByType;

    // Detailed Refund Reasons (for Table)
    private List<DetailedRefundReason> detailedRefundReasons;

    // Impact Analysis
    private BigDecimal revenueLostToRefunds;
    private Double revenueImpactPercentage;
    private Double avgRefundProcessingTime;
    private Double refundTrendPercentage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundTrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private Long count;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundReasonStats implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedRefundReason implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private Long count;
        private Double percentage;
        private BigDecimal totalAmount;
        private BigDecimal avgAmount;
    }
}