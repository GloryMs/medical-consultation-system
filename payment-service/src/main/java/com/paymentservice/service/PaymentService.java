package com.paymentservice.service;

import com.commonlibrary.dto.DoctorEarningsSummaryDto;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.exception.BusinessException;
import com.commonlibrary.dto.ChartDataPointDto;
import com.paymentservice.dto.EarningsReportDto;
import com.paymentservice.dto.PaymentHistoryDto;
import com.paymentservice.dto.PaymentReceiptDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import com.paymentservice.entity.Payment;
import com.commonlibrary.entity.PaymentMethod;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentDto processPayment(ProcessPaymentDto dto) {
        Payment payment = Payment.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .paymentType(dto.getPaymentType())
                .amount(dto.getAmount())
                .paymentMethod(PaymentMethod.valueOf(dto.getPaymentMethod().toUpperCase()))
                .status(PaymentStatus.PENDING)
                .currency("USD")
                .build();

        // Calculate platform fee (10%)
        BigDecimal platformFee = dto.getAmount().multiply(new BigDecimal("0.10"));
        payment.setPlatformFee(platformFee);
        payment.setDoctorAmount(dto.getAmount().subtract(platformFee));

        // Simulate payment processing
        simulatePaymentProcessing(payment);

        Payment saved = paymentRepository.save(payment);

        // Send Kafka event
        paymentEventProducer.sendPaymentCompletedEvent(
                dto.getPatientId(),
                dto.getDoctorId(),
                dto.getCaseId(),
                dto.getPaymentType().toString(),
                dto.getAmount().doubleValue(),
                saved.getTransactionId()
        );

        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(saved, PaymentDto.class);
    }

    private void simulatePaymentProcessing(Payment payment) {
        // Simulate payment gateway interaction
        payment.setTransactionId("TXN_" + System.currentTimeMillis());
        payment.setGatewayResponse("Payment successful");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
    }

    // 39. Get Payment History Implementation
    public List<PaymentHistoryDto> getPaymentHistory(Long userId) {
        // Get patient ID from user ID (in real implementation, this would be through a service call)
        List<Payment> payments = paymentRepository.findByPatientIdOrderByCreatedAtDesc(userId);

        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    // 40. Get Payment Receipt Implementation
    public PaymentReceiptDto getPaymentReceipt(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found", HttpStatus.NOT_FOUND));

        // Verify user has access to this payment
        if (!payment.getPatientId().equals(userId)) {
            throw new BusinessException("Unauthorized access to payment", HttpStatus.FORBIDDEN);
        }

        PaymentReceiptDto receipt = new PaymentReceiptDto();
        receipt.setReceiptNumber("RCP-" + payment.getId() + "-" + payment.getCreatedAt().toLocalDate());
        receipt.setPaymentId(payment.getId());
        receipt.setTransactionId(payment.getTransactionId());
        receipt.setPaymentDate(payment.getProcessedAt());
        receipt.setPaymentType(payment.getPaymentType().toString());
        receipt.setAmount(payment.getAmount());
        receipt.setPlatformFee(payment.getPlatformFee());
        receipt.setTotalAmount(payment.getAmount());
        receipt.setPaymentMethod( payment.getPaymentMethod().name());
        receipt.setStatus(payment.getStatus().toString());
        receipt.setCurrency(payment.getCurrency());

        // Add payer and payee details
        receipt.setPayerName("Patient ID: " + payment.getPatientId()); // In real app, fetch patient name
        if (payment.getDoctorId() != null) {
            receipt.setPayeeName("Doctor ID: " + payment.getDoctorId()); // In real app, fetch doctor name
        }

        return receipt;
    }

    public List<PaymentHistoryDto> getPatientPaymentHistory(Long patientId) {
        List<Payment> payments = paymentRepository.findByPatientId(patientId);
        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    public List<PaymentHistoryDto> getDoctorPaymentHistory(Long doctorId) {
        List<Payment> payments = paymentRepository.findByDoctorId(doctorId);
        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    public List<Payment> getAllPaymentsBetweenDates(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return paymentRepository.findByProcessedAtBetween(start, end);
    }

    public Double getTotalRevenue() {
        return paymentRepository.calculateTotalRevenue();
    }

    public Double getMonthlyRevenue() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        return paymentRepository.calculateRevenueBetween(startOfMonth, now);
    }

    public Map<String, Object> getPaymentDataBetweenDates(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Payment> payments = paymentRepository.findByProcessedAtBetween(start, end);

        // Calculate various metrics
        double totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        double subscriptionRevenue = payments.stream()
                .filter(p -> p.getPaymentType().name().equals("SUBSCRIPTION"))
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        double consultationRevenue = payments.stream()
                .filter(p -> p.getPaymentType().name().equals("CONSULTATION"))
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        data.put("totalRevenue", totalRevenue);
        data.put("subscriptionRevenue", subscriptionRevenue);
        data.put("consultationRevenue", consultationRevenue);
        data.put("platformFees", totalRevenue * 0.1);
        data.put("doctorPayouts", consultationRevenue * 0.9);
        data.put("refunds", 0.0); // Calculate actual refunds
        data.put("netRevenue", totalRevenue * 0.9);

        return data;
    }

    @Transactional
    public void processRefund(Long paymentId, Double amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Can only refund completed payments", HttpStatus.BAD_REQUEST);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);

        paymentRepository.save(payment);

        //log.info("Refund processed for payment {}: ${} - Reason: {}", paymentId, amount, reason);
    }

    /**
     * Get doctor payment history filtered by status
     */
    public List<PaymentHistoryDto> getDoctorPaymentHistoryByStatus(Long doctorId, PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByDoctorIdAndStatusOrderByProcessedAtDesc(doctorId, status);
        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get doctor payment history within date range
     */
    public List<PaymentHistoryDto> getDoctorPaymentHistoryByPeriod(
            Long doctorId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findByDoctorIdAndProcessedAtBetween(
                doctorId, startDate, endDate
        );
        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get comprehensive earnings summary for doctor
     */
    public DoctorEarningsSummaryDto getDoctorEarningsSummary(Long doctorId, String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = calculateStartDate(period, now);

        // Get all completed payments for the doctor
        List<Payment> allPayments = paymentRepository.findByDoctorIdAndStatusOrderByProcessedAtDesc(
                doctorId, PaymentStatus.COMPLETED
        );

        // Calculate total earnings
        BigDecimal totalEarnings = allPayments.stream()
                .map(Payment::getDoctorAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate period-specific earnings
        List<Payment> periodPayments = allPayments.stream()
                .filter(p -> p.getProcessedAt().isAfter(startDate))
                .collect(Collectors.toList());

        BigDecimal periodEarnings = periodPayments.stream()
                .map(Payment::getDoctorAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate weekly earnings
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        BigDecimal weeklyEarnings = allPayments.stream()
                .filter(p -> p.getProcessedAt().isAfter(oneWeekAgo))
                .map(Payment::getDoctorAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate monthly earnings
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        BigDecimal monthlyEarnings = allPayments.stream()
                .filter(p -> p.getProcessedAt().isAfter(oneMonthAgo))
                .map(Payment::getDoctorAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate today's earnings
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        BigDecimal todayEarnings = allPayments.stream()
                .filter(p -> p.getProcessedAt().isAfter(startOfDay))
                .map(Payment::getDoctorAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate pending payouts
        List<Payment> pendingPayments = paymentRepository.findByDoctorIdAndStatusOrderByProcessedAtDesc(
                doctorId, PaymentStatus.PENDING
        );
        BigDecimal pendingPayouts = pendingPayments.stream()
                .map(Payment::getDoctorAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate platform fees deducted
        BigDecimal platformFees = allPayments.stream()
                .map(Payment::getPlatformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average consultation fee
        BigDecimal avgFee = BigDecimal.ZERO;
        if (!allPayments.isEmpty()) {
            avgFee = totalEarnings.divide(
                    BigDecimal.valueOf(allPayments.size()),
                    2,
                    BigDecimal.ROUND_HALF_UP
            );
        }

        return DoctorEarningsSummaryDto.builder()
                .totalEarnings(totalEarnings)
                .monthlyEarnings(monthlyEarnings)
                .weeklyEarnings(weeklyEarnings)
                .todayEarnings(todayEarnings)
                .pendingPayouts(pendingPayouts)
                .completedConsultations((long) allPayments.size())
                .averageConsultationFee(avgFee)
                .platformFeesDeducted(platformFees)
                .period(period)
                .build();
    }

    /**
     * Helper method to calculate start date based on period
     */
    public LocalDateTime calculateStartDate(String period, LocalDateTime now) {
        switch (period.toLowerCase()) {
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "year":
                return now.minusYears(1);
            case "all":
            default:
                return LocalDateTime.of(2000, 1, 1, 0, 0); // Far past date for "all"
        }
    }

    /**
     * Get doctor earnings statistics
     */
    public Map<String, Object> getDoctorEarningsStats(Long doctorId) {
        Map<String, Object> stats = new HashMap<>();

        Double totalEarnings = paymentRepository.calculateDoctorTotalEarnings(doctorId);
        Long completedCount = paymentRepository.countCompletedPaymentsByDoctor(doctorId);
        Double averageEarnings = paymentRepository.calculateDoctorAverageEarnings(doctorId);

        stats.put("totalEarnings", totalEarnings != null ? totalEarnings : 0.0);
        stats.put("completedConsultations", completedCount != null ? completedCount : 0L);
        stats.put("averageEarnings", averageEarnings != null ? averageEarnings : 0.0);

        return stats;
    }

    // ... existing mapToHistoryDto method remains unchanged ...

    private PaymentHistoryDto mapToHistoryDto(Payment payment) {
        PaymentHistoryDto dto = new PaymentHistoryDto();
        dto.setId(payment.getId());
        dto.setPaymentType(payment.getPaymentType().toString());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus().toString());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setProcessedAt(payment.getProcessedAt());

        String description = payment.getPaymentType().name().equals("SUBSCRIPTION")
                ? "Subscription Payment"
                : "Consultation Payment - Case #" + payment.getCaseId();
        dto.setDescription(description);

        return dto;
    }

    /**
     * Generate comprehensive earnings report data
     */
    public EarningsReportDto generateEarningsReport(Long doctorId, String period,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate) {

        DoctorEarningsSummaryDto summary = getDoctorEarningsSummary(doctorId, period);
        List<PaymentHistoryDto> payments = getDoctorPaymentHistoryByPeriod(doctorId, startDate, endDate);
        Map<String, Object> stats = getDoctorEarningsStats(doctorId);

        return EarningsReportDto.builder()
                .summary(summary)
                .payments(payments)
                .statistics(stats)
                .generatedAt(LocalDateTime.now())
                .period(period)
                .startDate(startDate)
                .endDate(endDate)
                .doctorId(doctorId)
                .doctorName("Doctor ID: " + doctorId) // In production, fetch actual name
                .build();
    }

    /**
     * Get earnings data formatted for charts
     */
    public List<ChartDataPointDto> getEarningsChartData(Long doctorId, String period, String groupBy) {
        LocalDateTime start = calculateStartDate(period, LocalDateTime.now());
        LocalDateTime end = LocalDateTime.now();

        List<Payment> payments = paymentRepository.findByDoctorIdAndStatusAndProcessedAtBetween(
                doctorId, PaymentStatus.COMPLETED, start, end
        );

        // Group by date/week/month
        Map<LocalDate, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(p -> {
                    LocalDate date = p.getProcessedAt().toLocalDate();
                    if ("weekly".equals(groupBy)) {
                        return date.with(java.time.temporal.TemporalAdjusters.previousOrSame(
                                java.time.DayOfWeek.MONDAY));
                    } else if ("monthly".equals(groupBy)) {
                        return date.withDayOfMonth(1);
                    }
                    return date;
                }));

        // Transform to chart data format
        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal totalEarnings = entry.getValue().stream()
                            .map(Payment::getDoctorAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String label = formatDateLabel(entry.getKey(), groupBy);

                    return ChartDataPointDto.builder()
                            .date(entry.getKey().toString())
                            .label(label)
                            .earnings(totalEarnings)
                            .count(entry.getValue().size())
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /**
     * Get payment method distribution for pie chart
     */
    public List<ChartDataPointDto> getPaymentMethodDistribution(Long doctorId, String period) {
        LocalDateTime start = calculateStartDate(period, LocalDateTime.now());
        LocalDateTime end = LocalDateTime.now();

        List<Payment> payments = paymentRepository.findByDoctorIdAndStatusAndProcessedAtBetween(
                doctorId, PaymentStatus.COMPLETED, start, end
        );

        // Group by payment method
        Map<String, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentMethod().name()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal total = entry.getValue().stream()
                            .map(Payment::getDoctorAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return ChartDataPointDto.builder()
                            .name(formatPaymentMethodName(entry.getKey()))
                            .value(total)
                            .count(entry.getValue().size())
                            .build();
                })
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Generate CSV content from earnings report
     */
    public String generateEarningsCsv(EarningsReportDto reportData) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Earnings Report - ").append(reportData.getPeriod()).append("\n");
        csv.append("Generated: ").append(reportData.getGeneratedAt()).append("\n");
        csv.append("Period: ").append(reportData.getStartDate()).append(" to ")
                .append(reportData.getEndDate()).append("\n\n");

        // Summary
        csv.append("SUMMARY\n");
        csv.append("Total Earnings,").append(reportData.getSummary().getTotalEarnings()).append("\n");
        csv.append("Monthly Earnings,").append(reportData.getSummary().getMonthlyEarnings()).append("\n");
        csv.append("Weekly Earnings,").append(reportData.getSummary().getWeeklyEarnings()).append("\n");
        csv.append("Pending Payouts,").append(reportData.getSummary().getPendingPayouts()).append("\n");
        csv.append("Completed Consultations,").append(reportData.getSummary().getCompletedConsultations()).append("\n");
        csv.append("Average Fee,").append(reportData.getSummary().getAverageConsultationFee()).append("\n\n");

        // Payment History
        csv.append("PAYMENT HISTORY\n");
        csv.append("ID,Type,Amount,Status,Method,Date,Description\n");

        for (PaymentHistoryDto payment : reportData.getPayments()) {
            csv.append(payment.getId()).append(",")
                    .append(payment.getPaymentType()).append(",")
                    .append(payment.getAmount()).append(",")
                    .append(payment.getStatus()).append(",")
                    .append(payment.getPaymentMethod()).append(",")
                    .append(payment.getProcessedAt()).append(",")
                    .append("\"").append(payment.getDescription()).append("\"\n");
        }

        return csv.toString();
    }

    // Helper methods
    private String formatDateLabel(LocalDate date, String groupBy) {
        if ("monthly".equals(groupBy)) {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
        } else if ("weekly".equals(groupBy)) {
            return "Week of " + date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
        }
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
    }

    private String formatPaymentMethodName(String method) {
        if (method == null || method.isEmpty()) {
            return method;
        }

        // Split by underscore and capitalize each word
        String[] words = method.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                if (formatted.length() > 0) {
                    formatted.append(" ");
                }
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        return formatted.toString();
    }

}
