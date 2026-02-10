package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    // Transaction Volume Trend (for Multi-Line Chart)
    private List<TransactionVolumePoint> volumeTrend;

    // Transaction Stats
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private Double successRate;
    private Double failureRate;
    private Double avgProcessingTime;

    // Hourly Distribution (for Bar Chart)
    private Map<Integer, Long> hourlyDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionVolumePoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private Long successful;
        private Long failed;
        private Long pending;
    }
}