package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.patientservice.dto.PaymentHistoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @GetMapping("/api/payments/patient/{patientId}/history")
    //@GetMapping("/api/payments/history")
    ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPatientPaymentHistory(@PathVariable Long patientId);
}
