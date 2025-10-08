package com.paymentservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.DoctorEarningsSummaryDto;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.dto.ChartDataPointDto;
import com.paymentservice.dto.EarningsReportDto;
import com.paymentservice.dto.PaymentHistoryDto;
import com.paymentservice.dto.PaymentReceiptDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(
            @Valid @RequestBody ProcessPaymentDto dto) {
        PaymentDto payment = paymentService.processPayment(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment processed successfully"));
    }

    // 39. Get Payment History - MISSING ENDPOINT
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPaymentHistory(
            @RequestHeader("X-User-Id") Long userId) {
        List<PaymentHistoryDto> history = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // 40. Get Payment Receipt - MISSING ENDPOINT
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<ApiResponse<PaymentReceiptDto>> getPaymentReceipt(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long paymentId) {
        PaymentReceiptDto receipt = paymentService.getPaymentReceipt(userId, paymentId);
        return ResponseEntity.ok(ApiResponse.success(receipt));
    }

    // Additional endpoints for admin and internal use
    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPatientPaymentHistory(
            @PathVariable Long patientId) {
        List<PaymentHistoryDto> history = paymentService.getPatientPaymentHistory(patientId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/doctor/{doctorId}/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistory(
            @PathVariable Long doctorId) {
        List<PaymentHistoryDto> history = paymentService.getDoctorPaymentHistory(doctorId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Payment> payments = paymentService.getAllPaymentsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/revenue/total")
    public ResponseEntity<ApiResponse<Double>> getTotalRevenue() {
        Double revenue = paymentService.getTotalRevenue();
        return ResponseEntity.ok(ApiResponse.success(revenue));
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<ApiResponse<Double>> getMonthlyRevenue() {
        Double revenue = paymentService.getMonthlyRevenue();
        return ResponseEntity.ok(ApiResponse.success(revenue));
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentDataBetweenDates(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Map<String, Object> data = paymentService.getPaymentDataBetweenDates(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<Void>> processRefund(
            @PathVariable Long paymentId,
            @RequestParam Double amount,
            @RequestParam String reason) {
        paymentService.processRefund(paymentId, amount, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Refund processed"));
    }

    /**
     * Get doctor earnings summary
     */
    @GetMapping("/doctor/{doctorId}/earnings/summary")
    public ResponseEntity<ApiResponse<DoctorEarningsSummaryDto>> getDoctorEarningsSummary(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "month") String period) {
        DoctorEarningsSummaryDto summary = paymentService.getDoctorEarningsSummary(doctorId, period);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get doctor earnings statistics
     */
    @GetMapping("/doctor/{doctorId}/earnings/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorEarningsStats(
            @PathVariable Long doctorId) {
        Map<String, Object> stats = paymentService.getDoctorEarningsStats(doctorId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get doctor payment history by period
     */
    @GetMapping("/doctor/{doctorId}/history/period")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistoryByPeriod(
            @PathVariable Long doctorId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        List<PaymentHistoryDto> history = paymentService.getDoctorPaymentHistoryByPeriod(
                doctorId, start, end
        );
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Get doctor payment history by status
     */
    @GetMapping("/doctor/{doctorId}/history/status/{status}")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistoryByStatus(
            @PathVariable Long doctorId,
            @PathVariable String status) {
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
        List<PaymentHistoryDto> history = paymentService.getDoctorPaymentHistoryByStatus(
                doctorId, paymentStatus
        );
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Export earnings report as PDF
     */
    @GetMapping("/doctor/{doctorId}/earnings/export/pdf")
    public ResponseEntity<byte[]> exportEarningsReportPdf(
            @PathVariable Long doctorId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime start = startDate != null ?
                LocalDateTime.parse(startDate) :
                paymentService.calculateStartDate(period, LocalDateTime.now());
        LocalDateTime end = endDate != null ?
                LocalDateTime.parse(endDate) :
                LocalDateTime.now();

        EarningsReportDto reportData = paymentService.generateEarningsReport(
                doctorId, period, start, end
        );

        // For now, return a simple response - PDF generation can be added later
        String message = "PDF generation will be implemented with iText library";
        byte[] pdfBytes = message.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "earnings_report_" + doctorId + "_" + period + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Export earnings report as CSV
     */
    @GetMapping("/doctor/{doctorId}/earnings/export/csv")
    public ResponseEntity<String> exportEarningsReportCsv(
            @PathVariable Long doctorId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime start = startDate != null ?
                LocalDateTime.parse(startDate) :
                paymentService.calculateStartDate(period, LocalDateTime.now());
        LocalDateTime end = endDate != null ?
                LocalDateTime.parse(endDate) :
                LocalDateTime.now();

        EarningsReportDto reportData = paymentService.generateEarningsReport(
                doctorId, period, start, end
        );

        String csvContent = paymentService.generateEarningsCsv(reportData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                "earnings_report_" + doctorId + "_" + period + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Get chart data for earnings visualization
     */
    @GetMapping("/doctor/{doctorId}/earnings/chart-data")
    public ResponseEntity<ApiResponse<List<ChartDataPointDto>>> getEarningsChartData(
            @PathVariable Long doctorId,
            @RequestParam String period,
            @RequestParam(defaultValue = "daily") String groupBy) {

        List<ChartDataPointDto> chartData = paymentService.getEarningsChartData(
                doctorId, period, groupBy
        );
        return ResponseEntity.ok(ApiResponse.success(chartData));
    }

    /**
     * Get payment method distribution
     */
    @GetMapping("/doctor/{doctorId}/earnings/payment-methods")
    public ResponseEntity<ApiResponse<List<ChartDataPointDto>>> getPaymentMethodDistribution(
            @PathVariable Long doctorId,
            @RequestParam String period) {

        List<ChartDataPointDto> distribution = paymentService.getPaymentMethodDistribution(
                doctorId, period
        );
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }
}