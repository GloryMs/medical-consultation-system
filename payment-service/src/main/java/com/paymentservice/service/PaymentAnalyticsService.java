package com.paymentservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.PaymentMethod;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.repository.RefundLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentAnalyticsService {

    private final PaymentRepository paymentRepository;
    private final RefundLogRepository refundLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_DAYS_BACK = 30;

    /**
     * Get comprehensive payment analytics for the specified date range
     */
    public PaymentAnalyticsDto getPaymentAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating payment analytics from {} to {}", startDate, endDate);

        // Handle null dates - default to last 30 days
        if (startDate == null || endDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minusDays(DEFAULT_DAYS_BACK);
        }

        // Validate date range
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Count total payments analyzed
        Long totalPayments = paymentRepository.countTotalPaymentsInRange(startDate, endDate);

        return PaymentAnalyticsDto.builder()
                .totalPaymentsAnalyzed(totalPayments != null ? totalPayments : 0L)
                .overview(buildOverviewMetrics(startDate, endDate))
                .revenue(buildRevenueMetrics(startDate, endDate))
                .transactions(buildTransactionMetrics(startDate, endDate))
                .paymentMethods(buildPaymentMethodMetrics(startDate, endDate))
                .trends(buildTrendMetrics(startDate, endDate))
                .refunds(buildRefundMetrics(startDate, endDate))
                .startDate(startDate)
                .endDate(endDate)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Build overview metrics with KPIs
     */
    private PaymentOverviewMetrics buildOverviewMetrics(LocalDateTime start, LocalDateTime end) {
        // Get counts by status
        Long completedCount = paymentRepository.countByStatusAndDateRange(PaymentStatus.COMPLETED, start, end);
        Long failedCount = paymentRepository.countByStatusAndDateRange(PaymentStatus.FAILED, start, end);
        Long pendingCount = paymentRepository.countByStatusAndDateRange(PaymentStatus.PENDING, start, end);
        Long processingCount = paymentRepository.countByStatusAndDateRange(PaymentStatus.PROCESSING, start, end);
        Long refundedCount = paymentRepository.countByStatusAndDateRange(PaymentStatus.REFUNDED, start, end);

        Long totalPayments = safeSum(completedCount, failedCount, pendingCount, processingCount, refundedCount);

        // Get revenue
        BigDecimal totalRevenue = paymentRepository.getTotalRevenue(PaymentStatus.COMPLETED, start, end);
        totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;

        // Get refund stats
        Long totalRefunds = refundLogRepository.countCompletedRefunds(start, end);
        BigDecimal totalRefundedAmount = refundLogRepository.getTotalRefundedAmount(start, end);
        totalRefundedAmount = totalRefundedAmount != null ? totalRefundedAmount : BigDecimal.ZERO;

        // Calculate rates
        Double successRate = calculatePercentage(completedCount, totalPayments);
        Double refundRate = calculatePercentage(totalRefunds, totalPayments);

        // Average transaction value
        BigDecimal avgTransactionValue = completedCount != null && completedCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get distributions
        Map<String, Long> statusDistribution = buildStatusDistribution(start, end);
        Map<String, Long> typeDistribution = buildTypeDistribution(start, end);

        return PaymentOverviewMetrics.builder()
                .totalRevenue(totalRevenue)
                .totalPayments(totalPayments)
                .completedPayments(completedCount != null ? completedCount : 0L)
                .failedPayments(failedCount != null ? failedCount : 0L)
                .pendingPayments(safeSum(pendingCount, processingCount))
                .totalRefunds(totalRefunds != null ? totalRefunds : 0L)
                .successRate(successRate)
                .avgTransactionValue(avgTransactionValue)
                .totalRefundedAmount(totalRefundedAmount)
                .refundRate(refundRate)
                .statusDistribution(statusDistribution)
                .typeDistribution(typeDistribution)
                .build();
    }

    /**
     * Build revenue metrics with trends and breakdowns
     */
    private PaymentRevenueMetrics buildRevenueMetrics(LocalDateTime start, LocalDateTime end) {
        // Daily revenue trend
        List<PaymentRevenueMetrics.RevenueTrendPoint> revenueTrend = new ArrayList<>();
        List<Object[]> dailyRevenue = paymentRepository.getDailyRevenue(start, end);

        BigDecimal highestAmount = BigDecimal.ZERO;
        String highestDate = null;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int dayCount = 0;

        for (Object[] row : dailyRevenue) {
            LocalDate date = convertToLocalDate(row[0]);
            BigDecimal amount = convertToBigDecimal(row[1]);

            revenueTrend.add(PaymentRevenueMetrics.RevenueTrendPoint.builder()
                    .date(date.format(DATE_FORMATTER))
                    .revenue(amount)
                    .build());

            totalRevenue = totalRevenue.add(amount);
            dayCount++;

            if (amount.compareTo(highestAmount) > 0) {
                highestAmount = amount;
                highestDate = date.format(DATE_FORMATTER);
            }
        }

        // Revenue by type
        Map<String, BigDecimal> revenueByType = new HashMap<>();
        List<Object[]> revenueByTypeList = paymentRepository.getRevenueByType(start, end);
        for (Object[] row : revenueByTypeList) {
            PaymentType type = (PaymentType) row[0];
            BigDecimal amount = convertToBigDecimal(row[1]);
            revenueByType.put(type.name().toLowerCase(), amount);
        }

        // Calculate metrics
        BigDecimal avgDailyRevenue = dayCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Long completedCount = paymentRepository.countByStatusAndDateRange(PaymentStatus.COMPLETED, start, end);
        BigDecimal revenuePerTransaction = completedCount != null && completedCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Monthly comparison (current year vs previous year)
        List<PaymentRevenueMetrics.MonthlyRevenueComparison> monthlyComparison =
                buildMonthlyComparison(start, end);

        return PaymentRevenueMetrics.builder()
                .revenueTrend(revenueTrend)
                .revenueByType(revenueByType)
                .monthlyComparison(monthlyComparison)
                .highestRevenueDay(PaymentRevenueMetrics.HighestRevenueDay.builder()
                        .date(highestDate)
                        .amount(highestAmount)
                        .build())
                .avgDailyRevenue(avgDailyRevenue)
                .growthRate(0.0) // Not implemented (current period only)
                .revenuePerTransaction(revenuePerTransaction)
                .build();
    }

    /**
     * Build transaction metrics with volume trends
     */
    private PaymentTransactionMetrics buildTransactionMetrics(LocalDateTime start, LocalDateTime end) {
        // Volume trend by status
        List<PaymentTransactionMetrics.TransactionVolumePoint> volumeTrend = new ArrayList<>();
        List<Object[]> dailyTransactions = paymentRepository.getDailyTransactionsByStatus(start, end);

        // Group by date
        Map<String, Map<PaymentStatus, Long>> dailyData = new HashMap<>();
        for (Object[] row : dailyTransactions) {
            LocalDate date = convertToLocalDate(row[0]);
            PaymentStatus status = (PaymentStatus) row[1];
            Long count = (Long) row[2];

            String dateStr = date.format(DATE_FORMATTER);
            dailyData.putIfAbsent(dateStr, new HashMap<>());
            dailyData.get(dateStr).put(status, count);
        }

        // Build volume points
        for (Map.Entry<String, Map<PaymentStatus, Long>> entry : dailyData.entrySet()) {
            Map<PaymentStatus, Long> statusCounts = entry.getValue();
            volumeTrend.add(PaymentTransactionMetrics.TransactionVolumePoint.builder()
                    .date(entry.getKey())
                    .successful(statusCounts.getOrDefault(PaymentStatus.COMPLETED, 0L))
                    .failed(statusCounts.getOrDefault(PaymentStatus.FAILED, 0L))
                    .pending(safeSum(statusCounts.getOrDefault(PaymentStatus.PENDING, 0L),
                                    statusCounts.getOrDefault(PaymentStatus.PROCESSING, 0L)))
                    .build());
        }

        // Transaction stats
        Long totalTransactions = paymentRepository.countTotalPaymentsInRange(start, end);
        Long successfulTransactions = paymentRepository.countByStatusAndDateRange(PaymentStatus.COMPLETED, start, end);
        Long failedTransactions = paymentRepository.countByStatusAndDateRange(PaymentStatus.FAILED, start, end);

        Double successRate = calculatePercentage(successfulTransactions, totalTransactions);
        Double failureRate = calculatePercentage(failedTransactions, totalTransactions);

        // Hourly distribution
        Map<Integer, Long> hourlyDistribution = new HashMap<>();
        List<Object[]> hourlyData = paymentRepository.getHourlyDistribution(start, end);
        for (Object[] row : hourlyData) {
            // EXTRACT(HOUR FROM ...) returns Double in PostgreSQL
            Integer hour = convertToInteger(row[0]);
            Long count = (Long) row[1];
            hourlyDistribution.put(hour, count);
        }

        return PaymentTransactionMetrics.builder()
                .volumeTrend(volumeTrend)
                .totalTransactions(totalTransactions != null ? totalTransactions : 0L)
                .successfulTransactions(successfulTransactions != null ? successfulTransactions : 0L)
                .failedTransactions(failedTransactions != null ? failedTransactions : 0L)
                .successRate(successRate)
                .failureRate(failureRate)
                .avgProcessingTime(2.5) // Placeholder - would need to calculate from createdAt/processedAt
                .hourlyDistribution(hourlyDistribution)
                .build();
    }

    /**
     * Build payment method metrics
     */
    private PaymentMethodMetrics buildPaymentMethodMetrics(LocalDateTime start, LocalDateTime end) {
        List<PaymentMethodMetrics.PaymentMethodDistribution> methodDistribution = new ArrayList<>();
        List<PaymentMethodMetrics.PaymentMethodPerformance> methodPerformance = new ArrayList<>();
        Map<String, BigDecimal> revenueByMethod = new HashMap<>();

        // Get method stats
        List<Object[]> methodStats = paymentRepository.getPaymentMethodStats(start, end);
        List<Object[]> methodSuccessRates = paymentRepository.getPaymentMethodSuccessRates(start, end);
        List<Object[]> revenueByMethodData = paymentRepository.getRevenueByPaymentMethod(start, end);

        // Build revenue map
        for (Object[] row : revenueByMethodData) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal revenue = convertToBigDecimal(row[1]);
            revenueByMethod.put(formatPaymentMethod(method), revenue);
        }

        // Build success rate map
        Map<PaymentMethod, Double> successRateMap = new HashMap<>();
        for (Object[] row : methodSuccessRates) {
            PaymentMethod method = (PaymentMethod) row[0];
            Long successCount = (Long) row[1];
            Long totalCount = (Long) row[2];
            successRateMap.put(method, calculatePercentage(successCount, totalCount));
        }

        // Build distributions and performance
        for (Object[] row : methodStats) {
            PaymentMethod method = (PaymentMethod) row[0];
            Long count = (Long) row[1];
            BigDecimal sumAmount = convertToBigDecimal(row[2]);
            BigDecimal avgAmount = convertToBigDecimal(row[3]);

            String methodName = formatPaymentMethod(method);

            methodDistribution.add(PaymentMethodMetrics.PaymentMethodDistribution.builder()
                    .name(methodName)
                    .count(count)
                    .build());

            methodPerformance.add(PaymentMethodMetrics.PaymentMethodPerformance.builder()
                    .name(methodName)
                    .count(count)
                    .revenue(sumAmount)
                    .successRate(successRateMap.getOrDefault(method, 0.0))
                    .avgAmount(avgAmount)
                    .build());
        }

        // Gateway performance (Stripe-focused)
        List<PaymentMethodMetrics.GatewayPerformance> gatewayPerformance = buildStripeGatewayPerformance(start, end);

        return PaymentMethodMetrics.builder()
                .methodDistribution(methodDistribution)
                .revenueByMethod(revenueByMethod)
                .methodPerformance(methodPerformance)
                .gatewayPerformance(gatewayPerformance)
                .build();
    }

    /**
     * Build trend metrics with day-of-week analysis
     */
    private PaymentTrendMetrics buildTrendMetrics(LocalDateTime start, LocalDateTime end) {
        // Combined trend (revenue + transactions)
        List<PaymentTrendMetrics.CombinedTrendPoint> combinedTrend = new ArrayList<>();
        List<Object[]> dailyRevenue = paymentRepository.getDailyRevenue(start, end);

        // Build map of daily transactions
        Map<String, Long> dailyTransactionCounts = new HashMap<>();
        List<Object[]> dailyTransactions = paymentRepository.getDailyTransactionsByStatus(start, end);
        for (Object[] row : dailyTransactions) {
            LocalDate date = convertToLocalDate(row[0]);
            Long count = (Long) row[2];
            String dateStr = date.format(DATE_FORMATTER);
            dailyTransactionCounts.merge(dateStr, count, Long::sum);
        }

        for (Object[] row : dailyRevenue) {
            LocalDate date = convertToLocalDate(row[0]);
            BigDecimal revenue = convertToBigDecimal(row[1]);
            String dateStr = date.format(DATE_FORMATTER);

            combinedTrend.add(PaymentTrendMetrics.CombinedTrendPoint.builder()
                    .date(dateStr)
                    .revenue(revenue)
                    .transactions(dailyTransactionCounts.getOrDefault(dateStr, 0L))
                    .build());
        }

        // Day of week analysis
        List<PaymentTrendMetrics.DayOfWeekStats> dayOfWeekTrend = new ArrayList<>();
        List<Object[]> dayOfWeekData = paymentRepository.getDayOfWeekStats(start, end);

        for (Object[] row : dayOfWeekData) {
            // EXTRACT(DOW FROM ...) returns Double in PostgreSQL
            Integer dayNumber = convertToInteger(row[0]);
            BigDecimal revenue = convertToBigDecimal(row[1]);
            Long count = (Long) row[2];

            dayOfWeekTrend.add(PaymentTrendMetrics.DayOfWeekStats.builder()
                    .day(getDayName(dayNumber))
                    .revenue(revenue)
                    .transactions(count)
                    .build());
        }

        // Find peak day
        PaymentTrendMetrics.PeakInsight peakDay = dayOfWeekTrend.stream()
                .max(Comparator.comparing(PaymentTrendMetrics.DayOfWeekStats::getRevenue))
                .map(stats -> PaymentTrendMetrics.PeakInsight.builder()
                        .day(stats.getDay())
                        .revenue(stats.getRevenue())
                        .build())
                .orElse(null);

        return PaymentTrendMetrics.builder()
                .combinedTrend(combinedTrend)
                .dayOfWeekTrend(dayOfWeekTrend)
                .monthlyGrowth(new ArrayList<>()) // Not implemented (current period only)
                .peakDay(peakDay)
                .bestMonth(null) // Not implemented (current period only)
                .trendDirection("stable") // Not implemented (current period only)
                .forecastNextMonth(BigDecimal.ZERO) // Not implemented
                .seasonalAnalysis(new ArrayList<>()) // Not implemented
                .build();
    }

    /**
     * Build refund metrics
     */
    private PaymentRefundMetrics buildRefundMetrics(LocalDateTime start, LocalDateTime end) {
        // Refund stats
        Long totalRefunds = refundLogRepository.countCompletedRefunds(start, end);
        BigDecimal totalRefundAmount = refundLogRepository.getTotalRefundedAmount(start, end);
        BigDecimal avgRefundAmount = refundLogRepository.getAverageRefundAmount(start, end);

        totalRefundAmount = totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO;
        avgRefundAmount = avgRefundAmount != null ? avgRefundAmount : BigDecimal.ZERO;

        // Calculate refund rate
        Long totalPayments = paymentRepository.countTotalPaymentsInRange(start, end);
        Double refundRate = calculatePercentage(totalRefunds, totalPayments);

        // Daily refund trend
        List<PaymentRefundMetrics.RefundTrendPoint> refundTrend = new ArrayList<>();
        List<Object[]> dailyRefunds = refundLogRepository.getDailyRefundTrend(start, end);
        for (Object[] row : dailyRefunds) {
            LocalDate date = convertToLocalDate(row[0]);
            Long count = (Long) row[1];
            BigDecimal amount = convertToBigDecimal(row[2]);

            refundTrend.add(PaymentRefundMetrics.RefundTrendPoint.builder()
                    .date(date.format(DATE_FORMATTER))
                    .count(count)
                    .amount(amount)
                    .build());
        }

        // Refund reasons
        List<PaymentRefundMetrics.RefundReasonStats> refundReasons = new ArrayList<>();
        List<PaymentRefundMetrics.DetailedRefundReason> detailedRefundReasons = new ArrayList<>();
        List<Object[]> reasonStats = refundLogRepository.getRefundReasonStats(start, end);

        for (Object[] row : reasonStats) {
            String refundType = (String) row[0];
            Long count = (Long) row[1];
            BigDecimal amount = convertToBigDecimal(row[2]);

            String reasonName = formatRefundType(refundType);

            refundReasons.add(PaymentRefundMetrics.RefundReasonStats.builder()
                    .name(reasonName)
                    .count(count)
                    .build());

            Double percentage = totalRefunds != null && totalRefunds > 0
                    ? (count.doubleValue() / totalRefunds.doubleValue()) * 100.0
                    : 0.0;

            BigDecimal avgAmount = count > 0
                    ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            detailedRefundReasons.add(PaymentRefundMetrics.DetailedRefundReason.builder()
                    .name(reasonName)
                    .count(count)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .totalAmount(amount)
                    .avgAmount(avgAmount)
                    .build());
        }

        // Refunds by type
        Map<String, Long> refundsByType = new HashMap<>();
        List<Object[]> refundsByTypeData = refundLogRepository.getRefundsByPaymentType(start, end);
        for (Object[] row : refundsByTypeData) {
            PaymentType type = (PaymentType) row[0];
            Long count = (Long) row[1];
            refundsByType.put(type.name().toLowerCase(), count);
        }

        // Calculate impact
        BigDecimal totalRevenue = paymentRepository.getTotalRevenue(PaymentStatus.COMPLETED, start, end);
        totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;

        Double revenueImpactPercentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? (totalRefundAmount.doubleValue() / totalRevenue.doubleValue()) * 100.0
                : 0.0;

        return PaymentRefundMetrics.builder()
                .totalRefunds(totalRefunds != null ? totalRefunds : 0L)
                .totalRefundAmount(totalRefundAmount)
                .refundRate(refundRate)
                .avgRefundAmount(avgRefundAmount)
                .refundTrend(refundTrend)
                .refundReasons(refundReasons)
                .refundsByType(refundsByType)
                .detailedRefundReasons(detailedRefundReasons)
                .revenueLostToRefunds(totalRefundAmount)
                .revenueImpactPercentage(Math.round(revenueImpactPercentage * 10.0) / 10.0)
                .avgRefundProcessingTime(2.5) // Placeholder
                .refundTrendPercentage(0.0) // Not implemented (current period only)
                .build();
    }

    // =============== HELPER METHODS ===============

    private Map<String, Long> buildStatusDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> distribution = new HashMap<>();
        List<Object[]> statusData = paymentRepository.getStatusDistribution(start, end);
        for (Object[] row : statusData) {
            PaymentStatus status = (PaymentStatus) row[0];
            Long count = (Long) row[1];
            distribution.put(status.name().toLowerCase(), count);
        }
        return distribution;
    }

    private Map<String, Long> buildTypeDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> distribution = new HashMap<>();
        List<Object[]> typeData = paymentRepository.getTypeDistribution(start, end);
        for (Object[] row : typeData) {
            PaymentType type = (PaymentType) row[0];
            Long count = (Long) row[1];
            distribution.put(type.name().toLowerCase(), count);
        }
        return distribution;
    }

    private List<PaymentMethodMetrics.GatewayPerformance> buildStripeGatewayPerformance(
            LocalDateTime start, LocalDateTime end) {
        // Since we're Stripe-focused, aggregate all payment methods under Stripe
        Long totalTransactions = paymentRepository.countTotalPaymentsInRange(start, end);
        Long successfulTransactions = paymentRepository.countByStatusAndDateRange(PaymentStatus.COMPLETED, start, end);
        BigDecimal totalRevenue = paymentRepository.getTotalRevenue(PaymentStatus.COMPLETED, start, end);

        Double successRate = calculatePercentage(successfulTransactions, totalTransactions);

        return Arrays.asList(
                PaymentMethodMetrics.GatewayPerformance.builder()
                        .name("Stripe")
                        .transactions(totalTransactions != null ? totalTransactions : 0L)
                        .successRate(successRate)
                        .avgProcessingTime(2.1) // Placeholder
                        .revenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                        .build()
        );
    }

    private Double calculatePercentage(Long part, Long total) {
        if (total == null || total == 0 || part == null) {
            return 0.0;
        }
        double percentage = (part.doubleValue() / total.doubleValue()) * 100.0;
        return Math.round(percentage * 10.0) / 10.0; // Round to 1 decimal place
    }

    private Long safeSum(Long... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null) return "Unknown";
        return switch (method) {
            case CREDIT_CARD -> "Credit Card";
            case PAYPAL -> "PayPal";
            case BANK_TRANSFER -> "Bank Transfer";
            case STRIPE -> "Stripe";
            case COUPON -> "Coupon";
        };
    }

    private String formatRefundType(String refundType) {
        if (refundType == null || refundType.isEmpty()) return "Other";
        return switch (refundType) {
            case "DOCTOR_NO_SHOW" -> "Doctor No Show";
            case "INCOMPLETE_CONSULTATION" -> "Service Issue";
            case "PARTIAL_REFUND" -> "Partial Refund";
            case "BILLING_ERROR" -> "Billing Error";
            default -> refundType.replace("_", " ");
        };
    }

    private String getDayName(Integer dayNumber) {
        if (dayNumber == null) return "Unknown";
        // PostgreSQL EXTRACT(DOW): 0=Sunday, 1=Monday, ..., 6=Saturday
        return switch (dayNumber) {
            case 0 -> "Sunday";
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            default -> "Unknown";
        };
    }

    /**
     * Convert java.sql.Date (from database queries) to LocalDate
     */
    private LocalDate convertToLocalDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        // Handle java.sql.Date from database queries
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate();
        }

        // Handle LocalDate (if already converted)
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        }

        // Handle java.util.Date
        if (dateObj instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) dateObj).getTime()).toLocalDate();
        }

        throw new IllegalArgumentException("Cannot convert " + dateObj.getClass() + " to LocalDate");
    }

    /**
     * Safely convert Number (Double, BigDecimal, etc.) to BigDecimal
     * SQL aggregate functions like AVG() return Double, while SUM() returns BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object numberObj) {
        if (numberObj == null) {
            return BigDecimal.ZERO;
        }

        if (numberObj instanceof BigDecimal) {
            return (BigDecimal) numberObj;
        }

        if (numberObj instanceof Double) {
            return BigDecimal.valueOf((Double) numberObj);
        }

        if (numberObj instanceof Float) {
            return BigDecimal.valueOf(((Float) numberObj).doubleValue());
        }

        if (numberObj instanceof Long) {
            return BigDecimal.valueOf((Long) numberObj);
        }

        if (numberObj instanceof Integer) {
            return BigDecimal.valueOf((Integer) numberObj);
        }

        throw new IllegalArgumentException("Cannot convert " + numberObj.getClass() + " to BigDecimal");
    }

    /**
     * Safely convert Number to Integer
     * PostgreSQL EXTRACT function returns Double
     */
    private Integer convertToInteger(Object numberObj) {
        if (numberObj == null) {
            return 0;
        }

        if (numberObj instanceof Integer) {
            return (Integer) numberObj;
        }

        if (numberObj instanceof Double) {
            return ((Double) numberObj).intValue();
        }

        if (numberObj instanceof Float) {
            return ((Float) numberObj).intValue();
        }

        if (numberObj instanceof Long) {
            return ((Long) numberObj).intValue();
        }

        throw new IllegalArgumentException("Cannot convert " + numberObj.getClass() + " to Integer");
    }

    /**
     * Build monthly comparison between current year and previous year
     */
    private List<PaymentRevenueMetrics.MonthlyRevenueComparison> buildMonthlyComparison(
            LocalDateTime start, LocalDateTime end) {

        // Calculate previous year date range
        LocalDateTime previousYearStart = start.minusYears(1);
        LocalDateTime previousYearEnd = end.minusYears(1);

        // Get monthly revenue for current period
        List<Object[]> currentYearData = paymentRepository.getMonthlyRevenue(start, end);

        // Get monthly revenue for previous year period
        List<Object[]> previousYearData = paymentRepository.getMonthlyRevenue(previousYearStart, previousYearEnd);

        // Build maps: month -> revenue
        Map<Integer, BigDecimal> currentYearMap = new HashMap<>();
        for (Object[] row : currentYearData) {
            Integer month = convertToInteger(row[1]); // EXTRACT(MONTH) returns Double
            BigDecimal revenue = convertToBigDecimal(row[2]);
            currentYearMap.put(month, revenue);
        }

        Map<Integer, BigDecimal> previousYearMap = new HashMap<>();
        for (Object[] row : previousYearData) {
            Integer month = convertToInteger(row[1]);
            BigDecimal revenue = convertToBigDecimal(row[2]);
            previousYearMap.put(month, revenue);
        }

        // Build comparison list for all unique months
        Set<Integer> allMonths = new TreeSet<>();
        allMonths.addAll(currentYearMap.keySet());
        allMonths.addAll(previousYearMap.keySet());

        List<PaymentRevenueMetrics.MonthlyRevenueComparison> comparisons = new ArrayList<>();
        for (Integer month : allMonths) {
            String monthName = getMonthName(month);
            BigDecimal currentRevenue = currentYearMap.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal previousRevenue = previousYearMap.getOrDefault(month, BigDecimal.ZERO);

            comparisons.add(PaymentRevenueMetrics.MonthlyRevenueComparison.builder()
                    .month(monthName)
                    .currentYear(currentRevenue)
                    .previousYear(previousRevenue)
                    .build());
        }

        return comparisons;
    }

    /**
     * Get month name from month number (1-12)
     */
    private String getMonthName(Integer month) {
        if (month == null || month < 1 || month > 12) return "Unknown";
        return switch (month) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Unknown";
        };
    }
}