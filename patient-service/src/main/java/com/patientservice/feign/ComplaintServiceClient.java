package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.ComplaintDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "admin-service")
public interface ComplaintServiceClient {

    @GetMapping("/api/admin/complaints/{patientId}")
    ResponseEntity<ApiResponse<List<ComplaintDto>>> getPatientComplaintsById(
            @PathVariable Long patientId);

    @PostMapping("/api/admin/complaints")
    ResponseEntity<ApiResponse<Void>> submitComplaint(@RequestBody ComplaintDto complaintDto);

}
