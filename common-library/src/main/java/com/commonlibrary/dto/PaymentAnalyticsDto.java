package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAnalyticsDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long totalPaymentsAnalyzed;
    private PaymentOverviewMetrics overview;
    private PaymentRevenueMetrics revenue;
    private PaymentTransactionMetrics transactions;
    private PaymentMethodMetrics paymentMethods;
    private PaymentTrendMetrics trends;
    private PaymentRefundMetrics refunds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime generatedAt;
}