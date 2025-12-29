package com.adminservice.feign;

import com.adminservice.dto.*;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentAnalyticsDto;
import com.commonlibrary.dto.PaymentRecordDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {
    
    @GetMapping("/api/payments/revenue/total")
    Double getTotalRevenue();
    
    @GetMapping("/api/payments/revenue/monthly")
    Double getMonthlyRevenue();
    
    @GetMapping("/api/payments/consultation-fee/average")
    Double getAverageConsultationFee();
    
    @GetMapping("/api/payments/data")
    Map<String, Object> getPaymentDataBetweenDates(@RequestParam LocalDate startDate,
                                                   @RequestParam LocalDate endDate);
    
    @GetMapping("/api/payments/all")
    ResponseEntity<ApiResponse<List<PaymentRecordDto>>> getAllPayments(@RequestParam LocalDate startDate,
                                          @RequestParam LocalDate endDate);
    
    @GetMapping("/api/payments/subscriptions/all")
    List<SubscriptionPaymentDto> getSubscriptionPayments();
    
    @GetMapping("/api/payments/consultations/all")
    List<ConsultationPaymentDto> getConsultationPayments();
    
    @PostMapping("/api/payments/{paymentId}/refund")
    void processRefund(@PathVariable String paymentId,
                      @RequestParam Double amount,
                      @RequestParam String reason);

    @GetMapping("/api/admin/payments/analytics")
    ResponseEntity<ApiResponse<PaymentAnalyticsDto>> getPaymentAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);
}