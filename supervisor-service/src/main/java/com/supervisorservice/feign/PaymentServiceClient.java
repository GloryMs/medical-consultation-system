package com.supervisorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentHistoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for payment-service
 */
@FeignClient(
    name = "payment-service",
    url = "${payment-service.url:http://localhost:8084}",
    fallback = PaymentServiceClientFallback.class
)
public interface PaymentServiceClient {

    /**
     * Get payment history for a patient
     */
    @GetMapping("/api/payments/patient/{patientId}/history")
    ApiResponse<List<PaymentHistoryDto>> getPatientPaymentHistory(@PathVariable Long patientId);
}