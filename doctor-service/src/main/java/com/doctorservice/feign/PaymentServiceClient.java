package com.doctorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.dto.PaymentHistoryDto;
import com.commonlibrary.dto.ProcessPaymentDto;
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

    @GetMapping("/api/payments/doctor/{doctorId}/history")
    ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getDoctorPaymentHistory(@PathVariable Long doctorId);

    @PostMapping("/api/payments/process")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(@Valid @RequestBody ProcessPaymentDto dto);
}