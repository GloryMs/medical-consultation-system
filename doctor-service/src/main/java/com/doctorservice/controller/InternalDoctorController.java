package com.doctorservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.DoctorDetailsDto;
import com.doctorservice.entity.Appointment;
import com.doctorservice.entity.Doctor;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.service.DoctorService;
import com.doctorservice.service.InternalDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors-internal")
@RequiredArgsConstructor
@Slf4j
public class InternalDoctorController {
    
    private final InternalDoctorService internalDoctorService;
    private final InternalDoctorWorkloadController internalDoctorWorkloadController;
    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final AppointmentRepository appointmentRepository;
    
    @GetMapping("/pending-verifications")
    public List<PendingVerificationDto> getPendingVerifications() {
        return internalDoctorService.getPendingVerifications();
    }
    
    @GetMapping("/pending-verifications/count")
    public Long getPendingVerificationsCount() {
        return internalDoctorService.getPendingVerificationsCount();
    }
    
//    @GetMapping("/doctor-details/{doctorId}")
//    public DoctorDetailsDto getDoctorDetails(@PathVariable Long doctorId) {
//        return internalDoctorService.getDoctorDetails(doctorId);
//    }
    
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

    /**
     * Get doctor by ID with full details
     * Used for validation during case assignments
     */
    @GetMapping("/{doctorId}")
    @Operation(summary = "Get doctor by ID", description = "Retrieve complete doctor details by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorById(@PathVariable Long doctorId) {
        log.info("Fetching doctor details for doctorId: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Map<String, Object> doctorData = new HashMap<>();
        doctorData.put("id", doctor.getId());
        doctorData.put("userId", doctor.getUserId());
        doctorData.put("fullName", doctor.getFullName());
        doctorData.put("email", doctor.getEmail());
        doctorData.put("phoneNumber", doctor.getPhoneNumber());
        doctorData.put("verificationStatus", doctor.getVerificationStatus().name());
        doctorData.put("primarySpecialization", doctor.getPrimarySpecialization());
        doctorData.put("secondarySpecializations", doctor.getSubSpecializations());
        doctorData.put("licenseNumber", doctor.getLicenseNumber());
        doctorData.put("yearsOfExperience", doctor.getYearsOfExperience());
        doctorData.put("consultationFee", doctor.getCaseRate());
        doctorData.put("rating", doctor.getRating());
        doctorData.put("isAvailable", doctor.getIsAvailable());
        doctorData.put("currentCaseLoad", doctor.getWorkloadPercentage());
        doctorData.put("maxCaseLoad", doctor.getMaxActiveCases());

        return ResponseEntity.ok(ApiResponse.success(doctorData, "Doctor details retrieved"));
    }

    /**
     * Get basic doctor information
     * Lightweight endpoint for displaying doctor names in lists
     */
    @GetMapping("/{doctorId}/basic-info")
    @Operation(summary = "Get basic doctor info",
            description = "Retrieve basic doctor information (name, specialization, status)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorBasicInfo(@PathVariable Long doctorId) {
        log.info("Fetching basic info for doctorId: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("id", doctor.getId());
        basicInfo.put("userId", doctor.getUserId());
        basicInfo.put("fullName", doctor.getFullName());
        basicInfo.put("primarySpecialization", doctor.getPrimarySpecialization());
        basicInfo.put("verificationStatus", doctor.getVerificationStatus().name());
        basicInfo.put("isAvailable", doctor.getIsAvailable());
        basicInfo.put("rating", doctor.getRating());

        return ResponseEntity.ok(ApiResponse.success(basicInfo, "Basic info retrieved"));
    }

    /**
     * Get multiple doctors' basic info in batch
     * Optimized for fetching info for multiple doctors at once
     */
    @PostMapping("/basic-info/batch")
    @Operation(summary = "Get basic info for multiple doctors",
            description = "Batch endpoint for retrieving basic info for multiple doctors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDoctorsBasicInfoBatch(
            @RequestBody List<Long> doctorIds) {

        log.info("Fetching basic info for {} doctors", doctorIds.size());

        List<Doctor> doctors = doctorRepository.findAllById(doctorIds);

        List<Map<String, Object>> basicInfoList = doctors.stream()
                .map(doctor -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", doctor.getId());
                    info.put("fullName", doctor.getFullName());
                    info.put("primarySpecialization", doctor.getPrimarySpecialization());
                    info.put("verificationStatus", doctor.getVerificationStatus().name());
                    info.put("isAvailable", doctor.getIsAvailable());
                    return info;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(basicInfoList,
                String.format("Retrieved info for %d doctors", basicInfoList.size())));
    }

    /**
     * Check if doctor is verified and available
     * Quick validation endpoint for case assignment
     */
    @GetMapping("/{doctorId}/can-accept-cases")
    @Operation(summary = "Check if doctor can accept cases",
            description = "Validate if doctor is verified and available for case assignments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canAcceptCases(@PathVariable Long doctorId) {
        log.info("Checking if doctor {} can accept cases", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        boolean isVerified = "VERIFIED".equals(doctor.getVerificationStatus().name());
        boolean isAvailable = doctor.getIsAvailable();
        boolean hasCapacity = doctor.getWorkloadPercentage() < doctor.getMaxActiveCases();
        boolean canAcceptCases = isVerified && isAvailable && hasCapacity;

        Map<String, Object> result = new HashMap<>();
        result.put("canAcceptCases", canAcceptCases);
        result.put("isVerified", isVerified);
        result.put("isAvailable", isAvailable);
        result.put("hasCapacity", hasCapacity);
        result.put("currentCaseLoad", doctor.getWorkloadPercentage());
        result.put("maxCaseLoad", doctor.getMaxActiveCases());

        if (!canAcceptCases) {
            String reason = !isVerified ? "Not verified" :
                    !isAvailable ? "Not available" :
                            "Maximum case load reached";
            result.put("reason", reason);
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Validation completed"));
    }

    /**
     * Get doctors by verification status
     * Used by admin service for various queries
     */
    @GetMapping("/by-verification-status/{status}")
    @Operation(summary = "Get doctors by verification status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDoctorsByVerificationStatus(
            @PathVariable String status) {

        log.info("Fetching doctors with verification status: {}", status);

        List<Doctor> doctors = doctorRepository.findByVerificationStatus(
                com.commonlibrary.entity.VerificationStatus.valueOf(status)
        );

        List<Map<String, Object>> doctorList = doctors.stream()
                .map(doctor -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", doctor.getId());
                    info.put("fullName", doctor.getFullName());
                    info.put("email", doctor.getEmail());
                    info.put("primarySpecialization", doctor.getPrimarySpecialization());
                    info.put("verificationStatus", doctor.getVerificationStatus().name());
                    info.put("createdAt", doctor.getCreatedAt());
                    return info;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(doctorList,
                String.format("Retrieved %d doctors", doctorList.size())));
    }

    /**
     * Get doctors by specialization
     * Used for finding available doctors for case assignment
     */
    @GetMapping("/by-specialization/{specialization}")
    @Operation(summary = "Get doctors by specialization")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDoctorsBySpecialization(
            @PathVariable String specialization,
            @RequestParam(defaultValue = "false") boolean verifiedOnly) {

        log.info("Fetching doctors with specialization: {}, verifiedOnly: {}",
                specialization, verifiedOnly);

        List<Doctor> doctors;

        if (verifiedOnly) {
            doctors = doctorRepository.findByPrimarySpecializationAndVerificationStatus(
                    specialization,
                    com.commonlibrary.entity.VerificationStatus.VERIFIED
            );
        } else {
            doctors = doctorRepository.findByPrimarySpecialization(specialization);
        }

        List<Map<String, Object>> doctorList = doctors.stream()
                .map(doctor -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", doctor.getId());
                    info.put("fullName", doctor.getFullName());
                    info.put("primarySpecialization", doctor.getPrimarySpecialization());
                    info.put("verificationStatus", doctor.getVerificationStatus().name());
                    info.put("isAvailable", doctor.getIsAvailable());
                    info.put("currentCaseLoad", doctor.getWorkloadPercentage());
                    info.put("maxCaseLoad", doctor.getMaxActiveCases());
                    info.put("rating", doctor.getRating());
                    return info;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(doctorList,
                String.format("Retrieved %d doctors", doctorList.size())));
    }

    /**
     * Update doctor case load
     * Called when cases are assigned or completed
     */
    @PutMapping("/{doctorId}/case-load")
    @Operation(summary = "Update doctor case load",
            description = "Increment or decrement doctor's current case load")
    public ResponseEntity<ApiResponse<Void>> updateDoctorCaseLoad(
            @PathVariable Long doctorId,
            @RequestParam int change) {

        log.info("Updating case load for doctor {} by {}", doctorId, change);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        int newCaseLoad = Integer.valueOf( (doctor.getWorkloadPercentage().toString()) + change);

        // Validate case load doesn't go negative
        if (newCaseLoad < 0) {
            newCaseLoad = 0;
        }

        doctor.setWorkloadPercentage((double) newCaseLoad);
        doctorRepository.save(doctor);

        log.info("Updated doctor {} case load to {}", doctorId, newCaseLoad);

        return ResponseEntity.ok(ApiResponse.success(null, "Case load updated"));
    }

    /**
     * Get doctor statistics
     * Used for admin analytics and reporting
     */
    @GetMapping("/{doctorId}/statistics")
    @Operation(summary = "Get doctor statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorStatistics(@PathVariable Long doctorId) {
        log.info("Fetching statistics for doctor {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Map<String, Object> stats = new HashMap<>();
        stats.put("doctorId", doctor.getId());
        stats.put("fullName", doctor.getFullName());
        stats.put("totalCasesHandled", doctor.getConsultationCount());
        stats.put("activeCases", doctor.getActiveCases());
        stats.put("completedCases", doctor.getCompletionRate());
        stats.put("rating", doctor.getRating());
        stats.put("totalRatings", doctor.getTotalRatings());
        stats.put("yearsOfExperience", doctor.getYearsOfExperience());
        stats.put("verificationStatus", doctor.getVerificationStatus().name());
        stats.put("isAvailable", doctor.getIsAvailable());
        stats.put("joinedDate", doctor.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.success(stats, "Statistics retrieved"));
    }

    /////////



    /**
     * Get appointment by ID (internal use)
     */
    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointmentById(
            @PathVariable Long appointmentId) {

        log.info("Internal: Getting appointment by ID: {}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        AppointmentDto dto = doctorService.convertToAppointmentDto(appointment);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Get appointments by case ID (internal use)
     */
    @GetMapping("/appointments/case/{caseId}")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsByCaseId(
            @PathVariable Long caseId) {

        log.info("Internal: Getting appointments for case ID: {}", caseId);

        List<Appointment> appointments = appointmentRepository.findByCaseId(caseId);

        List<AppointmentDto> dtos = appointments.stream()
                .map(doctorService::convertToAppointmentDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Get patient appointments by status (internal use)
     */
    @GetMapping("/appointments/patient/{patientId}/status/{status}")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointmentsByStatus(
            @PathVariable Long patientId,
            @PathVariable AppointmentStatus status) {

        log.info("Internal: Getting appointments for patient {} with status {}", patientId, status);

        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatus(patientId, status);

        List<AppointmentDto> dtos = appointments.stream()
                .map(doctorService::convertToAppointmentDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Get all appointments for multiple patients (batch operation)
     */
    @PostMapping("/appointments/patients/batch")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsForPatients(
            @RequestBody List<Long> patientIds) {

        log.info("Internal: Getting appointments for {} patients", patientIds.size());

        List<Appointment> allAppointments = patientIds.stream()
                .flatMap(patientId -> appointmentRepository.findByPatientId(patientId).stream())
                .collect(Collectors.toList());

        List<AppointmentDto> dtos = allAppointments.stream()
                .map(doctorService::convertToAppointmentDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Count appointments by patient and status (internal use)
     */
    @GetMapping("/appointments/patient/{patientId}/count")
    public ResponseEntity<ApiResponse<Long>> countPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(required = false) AppointmentStatus status) {

        log.info("Internal: Counting appointments for patient {} with status {}", patientId, status);

        long count;
        if (status != null) {
            count = appointmentRepository.countByPatientIdAndStatus(patientId, status);
        } else {
            count = appointmentRepository.countByPatientId(patientId);
        }

        return ResponseEntity.ok(ApiResponse.success(count));
    }

}