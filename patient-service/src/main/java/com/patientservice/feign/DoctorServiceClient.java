package com.patientservice.feign;


import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.dto.DoctorDto;
import com.commonlibrary.entity.VerificationStatus;
import com.patientservice.entity.CaseStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "doctor-service")
public interface DoctorServiceClient {

    @GetMapping("/api/doctors/status/{status}/available/{isAvailable}")
    ResponseEntity<ApiResponse<List<DoctorDto>>> findByVerificationStatusAndIsAvailableTrue(
            @PathVariable VerificationStatus status, @PathVariable Boolean isAvailable);

    @PutMapping("/api/doctors/{doctorId}/update-load/{caseStatus}")
    ResponseEntity<ApiResponse<Void>> updateDoctorLoad(@PathVariable Long doctorId,
                                                       @PathVariable CaseStatus caseStatus,
                                                       @RequestParam int flag); // 1 -> increase | 0 -> decrease

    @GetMapping("/api/doctors/appointments/{patientId}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointments(@PathVariable Long patientId);

}
