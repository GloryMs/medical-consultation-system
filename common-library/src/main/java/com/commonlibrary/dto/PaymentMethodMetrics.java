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
public class PaymentMethodMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    // Method Distribution (for Pie Chart)
    private List<PaymentMethodDistribution> methodDistribution;

    // Revenue by Method (for Bar Chart)
    private Map<String, BigDecimal> revenueByMethod;

    // Method Performance Cards
    private List<PaymentMethodPerformance> methodPerformance;

    // Gateway Performance (for Table) - Stripe-focused
    private List<GatewayPerformance> gatewayPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDistribution implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodPerformance implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private Long count;
        private BigDecimal revenue;
        private Double successRate;
        private BigDecimal avgAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayPerformance implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private Long transactions;
        private Double successRate;
        private Double avgProcessingTime;
        private BigDecimal revenue;
    }
}