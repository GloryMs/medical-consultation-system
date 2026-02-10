package com.supervisorservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.CouponStatus;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import com.supervisorservice.dto.ApiResponse;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorPayment;
import com.supervisorservice.exception.ResourceNotFoundException;
import com.supervisorservice.feign.AdminServiceClient;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorCouponRepository;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import com.supervisorservice.repository.SupervisorPaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for admin operations on supervisors
 */
@RestController
@RequestMapping("/api/admin/supervisors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Supervisor Management", description = "Admin endpoints for supervisor management")
public class AdminSupervisorController {

    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final SupervisorCouponRepository couponRepository;
    private final SupervisorPaymentRepository paymentRepository;
    private final SupervisorKafkaProducer kafkaProducer;
    private final AdminServiceClient adminServiceClient;
    
    /**
     * Get all supervisors
     */
    @GetMapping
    @Operation(summary = "Get all supervisors", 
               description = "Retrieves all supervisors (Admin only)")
    public ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> getAllSupervisors(
            @RequestParam(required = false) SupervisorVerificationStatus status) {
        
        log.info("GET /api/admin/supervisors - status: {}", status);
        
        List<MedicalSupervisor> supervisors = status != null
                ? supervisorRepository.findByVerificationStatusAndIsDeletedFalse(status)
                : supervisorRepository.findByIsDeletedFalse();
        
        List<SupervisorProfileDto> dtos = supervisors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Get supervisors pending verification
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending supervisors", 
               description = "Retrieves supervisors pending verification (Admin only)")
    public ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> getPendingSupervisors() {
        
        log.info("GET /api/admin/supervisors/pending");
        
        List<MedicalSupervisor> supervisors = supervisorRepository.findPendingVerification();
        
        List<SupervisorProfileDto> dtos = supervisors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    /**
     * Get supervisor by ID
     */
    @GetMapping("/{supervisorId}")
    @Operation(summary = "Get supervisor by ID", 
               description = "Retrieves specific supervisor details (Admin only)")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> getSupervisor(
            @PathVariable Long supervisorId) {
        
        log.info("GET /api/admin/supervisors/{}", supervisorId);
        
        MedicalSupervisor supervisor = supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "id: " + supervisorId));
        
        return ResponseEntity.ok(ApiResponse.success(mapToDto(supervisor)));
    }
    
    /**
     * Verify supervisor
     */
    @PutMapping("/{supervisorId}/verify")
    @Operation(summary = "Verify supervisor", 
               description = "Verifies a supervisor application (Admin only)")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> verifySupervisor(
            @RequestHeader("X-User-Id") Long adminUserId,
            @PathVariable Long supervisorId,
            @Valid @RequestBody VerifySupervisorRequest request) {
        
        log.info("PUT /api/admin/supervisors/{}/verify - adminUserId: {}", supervisorId, adminUserId);
        
        MedicalSupervisor supervisor = supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "id: " + supervisorId));
        
        supervisor.setVerificationStatus(SupervisorVerificationStatus.VERIFIED);
        supervisor.setVerificationNotes(request.getVerificationNotes());
        supervisor.setVerifiedAt(LocalDateTime.now());
        supervisor.setVerifiedBy(adminUserId);
        supervisor.setIsAvailable(true);
        
        supervisor = supervisorRepository.save(supervisor);
        
        // Publish event
        kafkaProducer.sendSupervisorVerifiedEvent(supervisor, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success("Supervisor verified successfully", mapToDto(supervisor)));
    }
    
    /**
     * Reject supervisor
     */
    @PutMapping("/{supervisorId}/reject")
    @Operation(summary = "Reject supervisor", 
               description = "Rejects a supervisor application (Admin only)")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> rejectSupervisor(
            @PathVariable Long supervisorId,
            @Valid @RequestBody RejectSupervisorRequest request) {
        
        log.info("PUT /api/admin/supervisors/{}/reject", supervisorId);
        
        MedicalSupervisor supervisor = supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "id: " + supervisorId));
        
        supervisor.setVerificationStatus(SupervisorVerificationStatus.REJECTED);
        supervisor.setRejectionReason(request.getRejectionReason());
        supervisor.setIsAvailable(false);
        
        supervisor = supervisorRepository.save(supervisor);
        
        return ResponseEntity.ok(ApiResponse.success("Supervisor application rejected", mapToDto(supervisor)));
    }
    
    /**
     * Suspend supervisor
     */
    @PutMapping("/{supervisorId}/suspend")
    @Operation(summary = "Suspend supervisor", 
               description = "Suspends a supervisor account (Admin only)")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> suspendSupervisor(
            @PathVariable Long supervisorId,
            @RequestParam String reason) {
        
        log.info("PUT /api/admin/supervisors/{}/suspend - reason: {}", supervisorId, reason);
        
        MedicalSupervisor supervisor = supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "id: " + supervisorId));
        
        supervisor.setVerificationStatus(SupervisorVerificationStatus.SUSPENDED);
        supervisor.setIsAvailable(false);
        supervisor.setRejectionReason(reason);
        
        supervisor = supervisorRepository.save(supervisor);
        
        // Publish event
        kafkaProducer.sendSupervisorSuspendedEvent(supervisor, reason);
        
        return ResponseEntity.ok(ApiResponse.success("Supervisor suspended successfully", mapToDto(supervisor)));
    }
    
    /**
     * Update supervisor limits
     */
    @PutMapping("/{supervisorId}/limits")
    @Operation(summary = "Update supervisor limits", 
               description = "Updates supervisor patient and case limits (Admin only)")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> updateLimits(
            @PathVariable Long supervisorId,
            @Valid @RequestBody UpdateSupervisorLimitsRequest request) {
        
        log.info("PUT /api/admin/supervisors/{}/limits - maxPatients: {}, maxCases: {}", 
                supervisorId, request.getMaxPatientsLimit(), request.getMaxActiveCasesPerPatient());
        
        MedicalSupervisor supervisor = supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor", "id: " + supervisorId));
        
        supervisor.setMaxPatientsLimit(request.getMaxPatientsLimit());
        supervisor.setMaxActiveCasesPerPatient(request.getMaxActiveCasesPerPatient());
        
        supervisor = supervisorRepository.save(supervisor);
        
        return ResponseEntity.ok(ApiResponse.success("Supervisor limits updated successfully", mapToDto(supervisor)));
    }
    
    /**
     * Search supervisors
     */
    @GetMapping("/search")
    @Operation(summary = "Search supervisors", 
               description = "Searches supervisors by name, email, or organization (Admin only)")
    public ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> searchSupervisors(
            @RequestParam String query) {
        
        log.info("GET /api/admin/supervisors/search - query: {}", query);
        
        List<MedicalSupervisor> supervisors = supervisorRepository.searchSupervisors(query);
        
        List<SupervisorProfileDto> dtos = supervisors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
//    /**
//     * Issue single coupon
//     */
//    @PostMapping("/coupons")
//    @Operation(summary = "Issue coupon",
//               description = "Issues a single coupon for a patient (Admin only)")
//    public ResponseEntity<ApiResponse<CouponDto>> issueCoupon(
//            @RequestHeader("X-User-Id") Long adminUserId,
//            @Valid @RequestBody IssueCouponRequest request) {
//
//        log.info("POST /api/admin/supervisors/coupons - supervisorId: {}, patientId: {}",
//                request.getSupervisorId(), request.getPatientId());
//
//        CouponDto coupon = couponService.issueCoupon(request, adminUserId);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Coupon issued successfully", coupon));
//    }
    
//    /**
//     * Issue coupon batch
//     */
//    @PostMapping("/coupons/batch")
//    @Operation(summary = "Issue coupon batch",
//               description = "Issues multiple coupons for a patient (Admin only)")
//    public ResponseEntity<ApiResponse<List<CouponDto>>> issueCouponBatch(
//            @RequestHeader("X-User-Id") Long adminUserId,
//            @Valid @RequestBody IssueCouponBatchRequest request) {
//
//        log.info("POST /api/admin/supervisors/coupons/batch - supervisorId: {}, patientId: {}, count: {}",
//                request.getSupervisorId(), request.getPatientId(), request.getTotalCoupons());
//
//        List<CouponDto> coupons = couponService.issueCouponBatch(request, adminUserId);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success(
//                        String.format("%d coupons issued successfully", coupons.size()), coupons));
//    }
//
//    /**
//     * Cancel coupon
//     */
//    @PutMapping("/coupons/{couponId}/cancel")
//    @Operation(summary = "Cancel coupon",
//               description = "Cancels a coupon (Admin only)")
//    public ResponseEntity<ApiResponse<Void>> cancelCoupon(
//            @PathVariable Long couponId,
//            @Valid @RequestBody CancelCouponRequest request) {
//
//        log.info("PUT /api/admin/supervisors/coupons/{}/cancel - reason: {}",
//                couponId, request.getCancellationReason());
//
//        couponService.cancelCoupon(couponId, request.getCancellationReason());
//
//        return ResponseEntity.ok(ApiResponse.success("Coupon cancelled successfully", null));
//    }
    
    /**
     * Get supervisor statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get supervisor statistics",
               description = "Retrieves platform-wide supervisor statistics (Admin only)")
    public ResponseEntity<ApiResponse<SupervisorStatisticsDto>> getStatistics() {

        log.info("GET /api/admin/supervisors/statistics");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysFromNow = LocalDateTime.now().plusDays(7);

        // ===== Supervisor Metrics =====
        Long totalSupervisors = supervisorRepository.countTotalSupervisors();
        Long activeSupervisors = supervisorRepository.countActiveSupervisors();
        Long pendingSupervisors = supervisorRepository.countByVerificationStatusAndIsDeletedFalse(SupervisorVerificationStatus.PENDING);
        Long verifiedSupervisors = supervisorRepository.countByVerificationStatusAndIsDeletedFalse(SupervisorVerificationStatus.VERIFIED);
        Long rejectedSupervisors = supervisorRepository.countByVerificationStatusAndIsDeletedFalse(SupervisorVerificationStatus.REJECTED);
        Long suspendedSupervisors = supervisorRepository.countByVerificationStatusAndIsDeletedFalse(SupervisorVerificationStatus.SUSPENDED);

        // Build status breakdown map
        Map<String, Long> supervisorsByStatus = new HashMap<>();
        List<Object[]> statusStats = supervisorRepository.getVerificationStatusStatistics();
        for (Object[] stat : statusStats) {
            SupervisorVerificationStatus status = (SupervisorVerificationStatus) stat[0];
            Long count = (Long) stat[1];
            supervisorsByStatus.put(status.name(), count);
        }

        // ===== Patient Assignment Metrics =====
        Long totalPatientAssignments = assignmentRepository.countTotalAssignments();
        Long activePatientAssignments = assignmentRepository.countAllActiveAssignments();
        Long inactivePatientAssignments = assignmentRepository.countInactiveAssignments();
        Long totalUniquePatientsManaged = assignmentRepository.countUniquePatientsManaged();

        // Calculate average patients per supervisor
        Double averagePatientsPerSupervisor = activeSupervisors > 0
                ? (double) activePatientAssignments / activeSupervisors
                : 0.0;

        // ===== Coupon Metrics =====
        Long totalCouponsIssued = couponRepository.countTotalCoupons();
        Long availableCoupons = couponRepository.countByStatusAndIsDeletedFalse(CouponStatus.AVAILABLE);
        Long usedCoupons = couponRepository.countByStatusAndIsDeletedFalse(CouponStatus.USED);
        Long expiredCoupons = couponRepository.countByStatusAndIsDeletedFalse(CouponStatus.EXPIRED);
        Long cancelledCoupons = couponRepository.countByStatusAndIsDeletedFalse(CouponStatus.CANCELLED);
        Long couponsExpiringSoon = couponRepository.countCouponsExpiringSoon(sevenDaysFromNow);
        BigDecimal totalAvailableCouponValue = couponRepository.getGlobalTotalAvailableValue();

        // Build coupon status breakdown map
        Map<String, Long> couponsByStatus = new HashMap<>();
        List<Object[]> couponStatusStats = couponRepository.getCouponStatusStatistics();
        for (Object[] stat : couponStatusStats) {
            CouponStatus status = (CouponStatus) stat[0];
            Long count = (Long) stat[1];
            couponsByStatus.put(status.name(), count);
        }

        // ===== Payment Metrics =====
        Long totalPaymentsProcessed = paymentRepository.count();
        Long completedPayments = paymentRepository.countByStatus(SupervisorPayment.SupervisorPaymentStatus.COMPLETED);
        Long pendingPayments = paymentRepository.countByStatus(SupervisorPayment.SupervisorPaymentStatus.PENDING);
        Long failedPayments = paymentRepository.countByStatus(SupervisorPayment.SupervisorPaymentStatus.FAILED);
        BigDecimal totalAmountPaid = paymentRepository.getGlobalTotalAmountPaid();
        BigDecimal totalDiscountAmount = paymentRepository.getGlobalTotalDiscountAmount();
        BigDecimal averagePaymentAmount = paymentRepository.getAveragePaymentAmount();

        // Build payment method breakdown map
        Map<String, Long> paymentsByMethod = new HashMap<>();
        List<Object[]> paymentMethodStats = paymentRepository.getPaymentMethodStatistics();
        for (Object[] stat : paymentMethodStats) {
            SupervisorPayment.PaymentMethodType method = (SupervisorPayment.PaymentMethodType) stat[0];
            Long count = (Long) stat[1];
            paymentsByMethod.put(method.name(), count);
        }

        // ===== Capacity Metrics =====
        Long supervisorsWithCapacity = (long) supervisorRepository.findSupervisorsWithCapacity().size();
        Long totalPatientCapacity = supervisorRepository.getTotalPatientCapacity();
        Long usedPatientCapacity = activePatientAssignments;

        // Calculate average capacity utilization
        Double averageCapacityUtilization = totalPatientCapacity > 0
                ? (double) usedPatientCapacity / totalPatientCapacity * 100
                : 0.0;

        // ===== Recent Activity Metrics =====
        Long recentRegistrations = supervisorRepository.countRecentRegistrations(thirtyDaysAgo);
        Long recentVerifications = supervisorRepository.countRecentVerifications(thirtyDaysAgo);
        Long recentAssignments = assignmentRepository.countRecentAssignments(thirtyDaysAgo);
        Long recentPayments = paymentRepository.countRecentPayments(thirtyDaysAgo);

        // Build comprehensive statistics DTO
        SupervisorStatisticsDto statistics = SupervisorStatisticsDto.builder()
                // Supervisor Metrics
                .totalSupervisors(totalSupervisors)
                .activeSupervisors(activeSupervisors)
                .pendingSupervisors(pendingSupervisors)
                .verifiedSupervisors(verifiedSupervisors)
                .rejectedSupervisors(rejectedSupervisors)
                .suspendedSupervisors(suspendedSupervisors)
                .supervisorsByStatus(supervisorsByStatus)

                // Patient Assignment Metrics
                .totalPatientAssignments(totalPatientAssignments)
                .activePatientAssignments(activePatientAssignments)
                .inactivePatientAssignments(inactivePatientAssignments)
                .averagePatientsPerSupervisor(Math.round(averagePatientsPerSupervisor * 100.0) / 100.0)
                .totalUniquePatientsManaged(totalUniquePatientsManaged)

                // Coupon Metrics
                .totalCouponsIssued(totalCouponsIssued)
                .availableCoupons(availableCoupons)
                .usedCoupons(usedCoupons)
                .expiredCoupons(expiredCoupons)
                .cancelledCoupons(cancelledCoupons)
                .couponsExpiringSoon(couponsExpiringSoon)
                .totalAvailableCouponValue(totalAvailableCouponValue != null ? totalAvailableCouponValue : BigDecimal.ZERO)
                .couponsByStatus(couponsByStatus)

                // Payment Metrics
                .totalPaymentsProcessed(totalPaymentsProcessed)
                .completedPayments(completedPayments)
                .pendingPayments(pendingPayments)
                .failedPayments(failedPayments)
                .totalAmountPaid(totalAmountPaid != null ? totalAmountPaid : BigDecimal.ZERO)
                .totalDiscountAmount(totalDiscountAmount != null ? totalDiscountAmount : BigDecimal.ZERO)
                .paymentsByMethod(paymentsByMethod)
                .averagePaymentAmount(averagePaymentAmount != null ? averagePaymentAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)

                // Capacity Metrics
                .supervisorsWithCapacity(supervisorsWithCapacity)
                .averageCapacityUtilization(Math.round(averageCapacityUtilization * 100.0) / 100.0)
                .totalPatientCapacity(totalPatientCapacity)
                .usedPatientCapacity(usedPatientCapacity)

                // Recent Activity Metrics
                .recentRegistrations(recentRegistrations)
                .recentVerifications(recentVerifications)
                .recentAssignments(recentAssignments)
                .recentPayments(recentPayments)
                .build();

        log.info("Supervisor statistics generated: {} total supervisors, {} active, {} pending",
                totalSupervisors, activeSupervisors, pendingSupervisors);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
    
    /**
     * Map entity to DTO
     */
    private SupervisorProfileDto mapToDto(MedicalSupervisor supervisor) {
        return SupervisorProfileDto.builder()
                .id(supervisor.getId())
                .userId(supervisor.getUserId())
                .fullName(supervisor.getFullName())
                .organizationName(supervisor.getOrganizationName())
                .organizationType(supervisor.getOrganizationType())
                .licenseNumber(supervisor.getLicenseNumber())
                .licenseDocumentPath(supervisor.getLicenseDocumentPath())
                .phoneNumber(supervisor.getPhoneNumber())
                .email(supervisor.getEmail())
                .address(supervisor.getAddress())
                .city(supervisor.getCity())
                .country(supervisor.getCountry())
                .verificationStatus(supervisor.getVerificationStatus())
                .verificationNotes(supervisor.getVerificationNotes())
                .verifiedAt(supervisor.getVerifiedAt())
                .verifiedBy(supervisor.getVerifiedBy())
                .rejectionReason(supervisor.getRejectionReason())
                .maxPatientsLimit(supervisor.getMaxPatientsLimit())
                .maxActiveCasesPerPatient(supervisor.getMaxActiveCasesPerPatient())
                .isAvailable(supervisor.getIsAvailable())
                .activePatientCount(supervisor.getActivePatientCount())
                .availableCouponCount(supervisor.getAvailableCouponCount())
                .createdAt(supervisor.getCreatedAt())
                .updatedAt(supervisor.getUpdatedAt())
                .build();
    }

    //Submit new Complaints
    @PostMapping("/complaints")
    public ResponseEntity<com.commonlibrary.dto.ApiResponse<Void>> submitComplaint(
            @Valid @RequestBody ComplaintDto dto) {
        boolean success = false;
        success = adminServiceClient.submitComplaint(dto).getBody().isSuccess();
        if( success){
            return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.success(null, "Complaint submitted"));
        }
        return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.error("Something went wrong while submitting the complaint ..", HttpStatus.BAD_REQUEST));
    }

    //View My Complaints
    @GetMapping("/complaints")
    public ResponseEntity<com.commonlibrary.dto.ApiResponse<List<ComplaintDto>>> getMyComplaints(
            @RequestHeader("X-User-Id") Long userId) {
        List<ComplaintDto> complaints = adminServiceClient.getSupervisorComplaints(userId).getBody().getData();
        return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.success(complaints));
    }
}
