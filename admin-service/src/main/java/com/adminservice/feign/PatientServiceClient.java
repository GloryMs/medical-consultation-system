package com.adminservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {
    @GetMapping("/api/patients/cases/active/count")
    Long getActiveCasesCount();

    @GetMapping("/api/patients/cases/completed/count")
    Long getCompletedCasesCount();

    @GetMapping("/api/patients/subscriptions/active/count")
    Long getActiveSubscriptionsCount();

    @GetMapping("/api/patients/cases/total/count")
    Long getTotalCasesCount();

    @GetMapping("/api/patients/cases/in-progress/count")
    Long getCasesInProgressCount();

    @GetMapping("/api/patients/cases/average-resolution-time")
    Double getAverageCaseResolutionTime();
}
