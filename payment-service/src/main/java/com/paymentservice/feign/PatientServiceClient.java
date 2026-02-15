package com.paymentservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CustomPatientDto;
import com.commonlibrary.dto.RescheduleRequestResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {
    @GetMapping("/api/patients-internal/patient/{patientId}")
    ResponseEntity<ApiResponse<CustomPatientDto>> getPatientUserId( @PathVariable Long patientId);


}
