package com.doctorservice.feign;

import com.commonlibrary.dto.*;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @GetMapping("/api/payments/doctor/{doctorId}/history")
    ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistory(@PathVariable Long doctorId);

    @PostMapping("/api/payments/process")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(@Valid @RequestBody ProcessPaymentDto dto);

    @GetMapping("/api/payments/doctor/{doctorId}/earnings/summary")
    ResponseEntity<ApiResponse<DoctorEarningsSummaryDto>> getDoctorEarningsSummary(
            @PathVariable Long doctorId, @RequestParam(defaultValue = "month") String period);

    @GetMapping("/api/payments/doctor/{doctorId}/earnings/stats")
    ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorEarningsStats(@PathVariable Long doctorId);

    @GetMapping("/api/payments/doctor/{doctorId}/history/period")
    ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistoryByPeriod(
            @PathVariable Long doctorId,@RequestParam String startDate, @RequestParam String endDate);

    @GetMapping("/api/payments/doctor/{doctorId}/history/status/{status}")
    ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistoryByStatus(
            @PathVariable Long doctorId,@PathVariable String status );

    @GetMapping("/api/payments/doctor/{doctorId}/earnings/export/pdf")
    ResponseEntity<byte[]> exportEarningsReportPdf(
            @PathVariable Long doctorId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    );

    @GetMapping("/api/payments/doctor/{doctorId}/earnings/export/csv")
    ResponseEntity<String> exportEarningsReportCsv(
            @PathVariable Long doctorId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);

    @GetMapping("/api/payments/doctor/{doctorId}/earnings/chart-data")
    ResponseEntity<ApiResponse<List<ChartDataPointDto>>> getEarningsChartData(
            @PathVariable Long doctorId,
            @RequestParam String period,
            @RequestParam(defaultValue = "daily") String groupBy);

    @GetMapping("/api/payments/doctor/{doctorId}/earnings/payment-methods")
    ResponseEntity<ApiResponse<List<ChartDataPointDto>>> getPaymentMethodDistribution(
            @PathVariable Long doctorId,
            @RequestParam String period);

}