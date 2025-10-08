package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import com.commonlibrary.dto.PaymentHistoryDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @GetMapping("/api/payments/patient/{patientId}/history")
    ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPatientPaymentHistory(@PathVariable Long patientId);

    @PostMapping("/api/payments/process")
    ResponseEntity<ApiResponse<PaymentDto>> processPayment(@Valid @RequestBody ProcessPaymentDto dto);
}
