package com.doctorservice.controller;

import com.doctorservice.dto.DoctorDetailsDto;
import com.commonlibrary.dto.PendingVerificationDto;
import com.doctorservice.service.InternalDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class InternalDoctorController {
    
    private final InternalDoctorService internalDoctorService;
    
    @GetMapping("/pending-verifications")
    public List<PendingVerificationDto> getPendingVerifications() {
        return internalDoctorService.getPendingVerifications();
    }
    
    @GetMapping("/pending-verifications/count")
    public Long getPendingVerificationsCount() {
        return internalDoctorService.getPendingVerificationsCount();
    }
    
    @GetMapping("/{doctorId}")
    public DoctorDetailsDto getDoctorDetails(@PathVariable Long doctorId) {
        return internalDoctorService.getDoctorDetails(doctorId);
    }
    
    @GetMapping("/{doctorId}/performance")
    public Map<String, Object> getDoctorPerformance(@PathVariable Long doctorId,
                                                    @RequestParam LocalDate startDate,
                                                    @RequestParam LocalDate endDate) {
        return internalDoctorService.getDoctorPerformance(doctorId, startDate, endDate);
    }
}