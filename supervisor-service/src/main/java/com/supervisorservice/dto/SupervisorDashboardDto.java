package com.supervisorservice.dto;

import com.commonlibrary.entity.SupervisorVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for supervisor dashboard statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorDashboardDto {

    private Long supervisorId;
    private String supervisorName;
    private SupervisorVerificationStatus verificationStatus;
    private int activePatientCount;
    private int totalCasesSubmitted;
    private int totalCouponsIssued;
    private BigDecimal totalCouponValue;
    private Integer totalPaymentsProcessed;
    private LocalDateTime lastActivityAt;
    // Patient Statistics
    private Integer totalPatients;
    private Integer activePatients;
    private Integer maxPatientsLimit;
    
    // Case Statistics
    private Integer totalCases;
    private Integer activeCases;
    private Integer completedCases;
    private Integer pendingCases;
    
    // Appointment Statistics
    private Integer upcomingAppointments;
    private Integer completedAppointments;
    private Integer totalAppointments;
    
    // Coupon Statistics
    private Integer totalCoupons;
    private Integer availableCoupons;
    private Integer usedCoupons;
    private Integer expiredCoupons;
    private BigDecimal availableCouponValue;
    
    // Payment Statistics
    private Integer totalPayments;
    private Integer directPayments;
    private Integer couponPayments;
    private BigDecimal totalPaymentAmount;
    
    // Recent Activity
    private List<RecentActivityDto> recentActivities;
    
    // Coupon Expiring Soon
    private List<CouponDto> couponsExpiringSoon;
    
    // Profile Completion
    private Integer profileCompletionPercentage;
    private List<String> missingProfileFields;
}
