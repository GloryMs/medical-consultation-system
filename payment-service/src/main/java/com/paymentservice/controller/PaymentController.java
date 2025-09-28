package com.paymentservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentDto;
import com.paymentservice.dto.PaymentHistoryDto;
import com.paymentservice.dto.PaymentReceiptDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
}