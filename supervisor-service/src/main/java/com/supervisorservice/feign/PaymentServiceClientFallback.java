package com.supervisorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentHistoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback implementation for PaymentServiceClient
 */
@Component
@Slf4j
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public ApiResponse<List<PaymentHistoryDto>> getPatientPaymentHistory(Long patientId) {
        log.error("PaymentService unavailable - getPatientPaymentHistory fallback for patientId: {}", patientId);
        return ApiResponse.error("Payment service unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}