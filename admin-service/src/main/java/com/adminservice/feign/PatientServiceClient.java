package com.adminservice.feign;

import com.commonlibrary.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {
//    @GetMapping("/api/patients/cases/active/count")
//    Long getActiveCasesCount();
//
//    @GetMapping("/api/patients/cases/completed/count")
//    Long getCompletedCasesCount();
//
//    @GetMapping("/api/patients/subscriptions/active/count")
//    Long getActiveSubscriptionsCount();
//
//    @GetMapping("/api/patients/cases/total/count")
//    Long getTotalCasesCount();
//
//    @GetMapping("/api/patients/cases/in-progress/count")
//    Long getCasesInProgressCount();
//
//    @GetMapping("/api/patients/cases/average-resolution-time")
//    Double getAverageCaseResolutionTime();

    @GetMapping("/api/patients/cases/all-metrics")
    ResponseEntity<ApiResponse<Map<String,Long>>> getAllMetrics();

}
