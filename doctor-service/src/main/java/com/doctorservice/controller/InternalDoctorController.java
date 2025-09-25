package com.doctorservice.controller;

import com.commonlibrary.dto.DoctorCapacityDto;
import com.commonlibrary.dto.DoctorDto;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.DoctorDetailsDto;
import com.commonlibrary.dto.PendingVerificationDto;
import com.doctorservice.service.DoctorService;
import com.doctorservice.service.InternalDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors-internal")
@RequiredArgsConstructor
public class InternalDoctorController {
    
    private final InternalDoctorService internalDoctorService;
    private final InternalDoctorWorkloadController internalDoctorWorkloadController;
    
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

    @GetMapping ("/specialization/{specialization}/with-capacity")
    public List<DoctorCapacityDto> getAvailableDoctorsBySpecializationWithCapacity(@PathVariable String specialization,
                                                              @RequestParam(defaultValue = "10") int limit) {
        List<DoctorCapacityDto> doctorsCapacities = new ArrayList<>();
        List<DoctorDto> doctors = new ArrayList<>();
        doctors = internalDoctorService.getDoctorsBySpecialization( specialization, limit );

        if( doctors != null && !doctors.isEmpty() ) {
            List<Long> doctorIds = new ArrayList<>();
            for( DoctorDto doctor : doctors ) {
                doctorIds.add(doctor.getId());
            }
            try{
                doctorsCapacities = internalDoctorWorkloadController.getBatchCapacity(doctorIds).getBody().getData();
                for( DoctorDto doctor : doctors ) {
                    doctorsCapacities.stream().filter(d-> d.getDoctorId() == doctor.getId()).
                            findFirst().ifPresent(d -> {
                                d.setFullName(doctor.getFullName());
                                System.out.println( "While getting doctors with capacity - Doctor: " + d.getFullName());
                                d.setAverageRating(doctor.getRating());
                                d.setYearsOfExperience(doctor.getYearsOfExperience());
                                d.setCompletionRate(doctor.getCompletionRate());
                                d.setPrimarySpecialization(doctor.getPrimarySpecialization());
                                d.setSubSpecializations(doctor.getSubSpecializations());
                                d.setEmergencyMode(doctor.getEmergencyMode());
                                d.setConsultationCount(doctor.getConsultationCount());
                                d.setCompletionRate(doctor.getCompletionRate());
                                d.setWorkloadPercentage(doctor.getWorkloadPercentage());
                            });
                }
            } catch (Exception e) {
                System.out.println("Error while loading doctors with capacity for specialization: " + specialization);
                 e.printStackTrace();
            }
        }
        return doctorsCapacities;
    }

}