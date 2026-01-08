package com.supervisorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.dto.PaymentHistoryDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for payment-service
 */
@FeignClient(
    name = "payment-service")
public interface PaymentServiceClient {

    /**
     * Get payment history for a patient
     */
    @GetMapping("/api/payments/patient/{patientId}/history")
    ApiResponse<List<PaymentHistoryDto>> getPatientPaymentHistory(@PathVariable Long patientId);

    /**
     * Process a payment
     */
    @PostMapping("/process")
    ResponseEntity<ApiResponse<PaymentDto>> processPayment(
            @Valid @RequestBody ProcessPaymentDto dto);

    /**
     * Process payment on behalf of patient (supervisor initiated)
     */
    @PostMapping("/process/supervisor")
    ResponseEntity<ApiResponse<PaymentDto>> processPaymentBySupervisor(
            @Valid @RequestBody ProcessPaymentDto dto,
            @RequestHeader("X-Supervisor-Id") Long supervisorId);

    /**
     * Create payment intent for consultation
     */
    @PostMapping("/consultation/create-intent")
    ResponseEntity<ApiResponse<Map<String, Object>>> createConsultationPaymentIntent(
            @RequestParam Long caseId,
            @RequestParam Long patientId,
            @RequestParam Long doctorId,
            @RequestHeader("X-Supervisor-Id") Long supervisorId);

    /**
     * Confirm consultation payment
     */
    @PostMapping("/consultation/confirm")
    ResponseEntity<ApiResponse<PaymentDto>> confirmConsultationPayment(
            @RequestParam Long paymentId,
            @RequestParam String paymentMethodId);

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(@PathVariable Long paymentId);


    /**
     * Get payment by Stripe payment intent ID
     */
    @GetMapping("/stripe/intent/{paymentIntentId}")
    ResponseEntity<ApiResponse<PaymentDto>> getPaymentByStripeIntentId(
            @PathVariable String paymentIntentId);

    /**
     * Cancel a pending payment
     */
    @PostMapping("/{paymentId}/cancel")
    ResponseEntity<ApiResponse<Void>> cancelPayment(
            @PathVariable Long paymentId,
            @RequestParam String reason);

    /**
     * Request refund for a payment
     */
    @PostMapping("/{paymentId}/refund")
    ResponseEntity<ApiResponse<PaymentDto>> requestRefund(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason);
}