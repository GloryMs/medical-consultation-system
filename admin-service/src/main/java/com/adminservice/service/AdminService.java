package com.adminservice.service;

import com.adminservice.dto.*;
import com.adminservice.entity.*;
import com.adminservice.feign.*;
import com.adminservice.kafka.AdminEventProducer;
import com.adminservice.repository.ComplaintRepository;
import com.adminservice.repository.SystemConfigRepository;
import com.adminservice.repository.StaticContentRepository;
import com.adminservice.repository.UserRepository;
import com.commonlibrary.dto.PendingVerificationDto;
import com.commonlibrary.dto.DoctorDto;
import com.commonlibrary.dto.*;
import com.commonlibrary.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final DoctorServiceClient doctorServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final AuthServiceClient authServiceClient;
    private final ComplaintRepository complaintRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final StaticContentRepository staticContentRepository;
    private final UserRepository userRepository;
    private final AdminEventProducer adminEventProducer;
    private final NotificationServiceClient notificationServiceClient;

    public DashboardDto getDashboard() {
        // Simulate dashboard data
        return DashboardDto.builder()
                .totalUsers(150L)
                .totalDoctors(50L)
                .totalPatients(100L)
                .activeCases(25L)
                .completedCases(75L)
                .pendingVerifications(5L)
                .totalRevenue(15000.00)
                .activeSubscriptions(85L)
                .build();
    }

    public UserStatsDto getUserStats(){
        UserStatsDto statsDto = new UserStatsDto();
        try{
            statsDto = authServiceClient.getUsersStats().getBody().getData();
        } catch (Exception e) {
            log.error("Failed to get users stats ..");
            System.out.println("Failed to get users stats ..");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return statsDto;
    }

    public void verifyDoctor(VerifyDoctorDto dto) {
        try{
            doctorServiceClient.updateDoctorVerification(
                    dto.getDoctorId(),
                    dto.getApproved() ? "VERIFIED" : "REJECTED",
                    dto.getReason()
            );
        } catch (Exception e) {
            log.error("Failed to verify doctor", e);
            log.error(e.getMessage());
        }
    }

    // 26. Get System Metrics Implementation
    public SystemMetricsDto getSystemMetrics() {
        SystemMetricsDto metrics = new SystemMetricsDto();

        // User metrics
        metrics.setTotalUsers(userRepository.count());
        metrics.setActiveUsers(userRepository.countByStatus("ACTIVE"));
        //metrics.setNewUsersToday(userRepository.countUsersCreatedToday());
        //metrics.setNewUsersThisMonth(userRepository.countUsersCreatedThisMonth());

        metrics.setNewUsersToday(Long.parseLong("2.0"));
        metrics.setNewUsersThisMonth(Long.parseLong("10.0"));

        // Case metrics
        CaseMetricsDto metricsMap = new CaseMetricsDto();
        try{

            String startDate = "2025-01-01";
            String endDate = "2025-12-31";

            metricsMap = patientServiceClient.getCaseMetrics(startDate, endDate).getBody().getData();
            metrics.setTotalCases(metricsMap.getTotalCases());
            metrics.setCasesInProgress(metricsMap.getInProgressCount());
            metrics.setAverageCaseResolutionTime(Double.valueOf("2.5"));
        } catch (Exception e) {
            log.error("Failed to retrieve cases metrics", e);
            log.error(e.getMessage());
        }

        /*TODO
        *  Enable remaining metrics*/
        // Financial metrics
//        metrics.setTotalRevenue(paymentServiceClient.getTotalRevenue());
//        metrics.setRevenueThisMonth(paymentServiceClient.getMonthlyRevenue());
//        metrics.setAverageConsultationFee(paymentServiceClient.getAverageConsultationFee());
//
//        // System health
//        metrics.setSystemUptime(calculateSystemUptime());
//        metrics.setActiveServices(getActiveServicesCount());
//        metrics.setErrorRate(calculateErrorRate());

        return metrics;
    }

    // 27. Generate Revenue Report Implementation
    public RevenueReportDto generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        RevenueReportDto report = new RevenueReportDto();

        report.setStartDate(startDate);
        report.setEndDate(endDate);

        // Get payment data from payment service
        Map<String, Object> paymentData = paymentServiceClient.getPaymentDataBetweenDates(startDate, endDate);

        report.setTotalRevenue((Double) paymentData.get("totalRevenue"));
        report.setSubscriptionRevenue((Double) paymentData.get("subscriptionRevenue"));
        report.setConsultationRevenue((Double) paymentData.get("consultationRevenue"));
        report.setPlatformFees((Double) paymentData.get("platformFees"));
        report.setDoctorPayouts((Double) paymentData.get("doctorPayouts"));
        report.setRefunds((Double) paymentData.get("refunds"));
        report.setNetRevenue((Double) paymentData.get("netRevenue"));

        // Daily breakdown
        report.setDailyBreakdown((List<DailyRevenueDto>) paymentData.get("dailyBreakdown"));

        // Top performers
        report.setTopDoctorsByRevenue((List<DoctorRevenueDto>) paymentData.get("topDoctors"));
        report.setTopPatientsBySpending((List<PatientSpendingDto>) paymentData.get("topPatients"));

        return report;
    }

    // 25 & 28. Get Pending Verifications Implementation
    public List<PendingVerificationDto> getPendingVerifications() {
        List<PendingVerificationDto> pendingVerifications = new ArrayList<>();
        try{
            pendingVerifications= doctorServiceClient.getPendingVerifications().getBody().getData();
        }
        catch(Exception e){
            log.error("Failed to get doctors with pending verification");
            log.error(e.getMessage());
        }
        return pendingVerifications;
    }

    /**
     * Get Doctor Verification Details
     * Retrieves complete doctor information from doctor-service for admin verification
     *
     * @param doctorId The ID of the doctor to retrieve details for
     * @return Complete doctor verification details
     * @throws BusinessException if doctor not found or service unavailable
     */
    public DoctorVerificationDetailsDto getDoctorVerificationDetails(Long doctorId) {
        try {
            log.info("Fetching verification details for doctor ID: {}", doctorId);

            // Call doctor-service via Feign client
            ResponseEntity<ApiResponse<DoctorVerificationDetailsDto>> response =
                    doctorServiceClient.getDoctorVerificationDetails(doctorId);

            if (response.getBody() != null && response.getBody().getData() != null) {
                DoctorVerificationDetailsDto details = response.getBody().getData();
                log.info("Successfully retrieved verification details for doctor: {}", details.getFullName());
                return details;
            } else {
                log.error("No data returned from doctor-service for doctor ID: {}", doctorId);
                throw new BusinessException("Doctor verification details not found", HttpStatus.NOT_FOUND);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching doctor verification details for ID {}: {}", doctorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to retrieve doctor verification details: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public PendingVerificationDto convertToPendingVerificationDto(DoctorDto doctor) {
        PendingVerificationDto pendingVerificationDto = new PendingVerificationDto();
        pendingVerificationDto.setDoctorId(doctor.getId());
        pendingVerificationDto.setFullName(doctor.getFullName());
        pendingVerificationDto.setLicenseNumber(doctor.getLicenseNumber());
        pendingVerificationDto.setSpecialization(doctor.getPrimarySpecialization());
        //pendingVerificationDto.setSubmittedAt(doctor.getcr());
        pendingVerificationDto.setDocumentsUrl("To be changed .. added by me :)");
         return pendingVerificationDto;
    }

//    // 29. Get All Users Implementation
//    public Page<UserDto> getAllUsers(int page, int size, String role, String status) {
//        Pageable pageable = PageRequest.of(page, size);
//
//        if (role != null && status != null) {
//            return userRepository.findByRoleAndStatus(role, status, pageable)
//                    .map(this::mapToUserDto);
//        } else if (role != null) {
//            return userRepository.findByRole(role, pageable)
//                    .map(this::mapToUserDto);
//        } else if (status != null) {
//            return userRepository.findByStatus(status, pageable)
//                    .map(this::mapToUserDto);
//        } else {
//            return userRepository.findAll(pageable)
//                    .map(this::mapToUserDto);
//        }
//    }

    public List<NotificationDto> getMyNotificationsByUserId(Long userId){
        UserDetailsNew adminUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Admin user not found", HttpStatus.NOT_FOUND));
        List<NotificationDto> dtos = new ArrayList<>();
        try{
            dtos = notificationServiceClient.getUserNotifications(adminUser.getId()).getBody().getData();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return dtos;
    }

    public DoctorDetailsDto convertToDoctorDetailsDto(DoctorDto doctor){
        DoctorDetailsDto doctorDetailsDto = new DoctorDetailsDto();
        doctorDetailsDto.setId(doctor.getUserId());
        doctorDetailsDto.setId(doctor.getUserId());
        doctorDetailsDto.setFullName(doctor.getFullName());
        doctorDetailsDto.setLicenseNumber(doctor.getLicenseNumber());
        doctorDetailsDto.setPrimarySpecialization(doctor.getPrimarySpecialization());
        doctorDetailsDto.setSubSpecialization("Sub Specialization .. Added by me");
        doctorDetailsDto.setAverageRating(doctor.getRating());
        doctorDetailsDto.setConsultationCount(doctor.getConsultationCount());
        return doctorDetailsDto;
    }

    // 30. Generate Doctor Performance Report Implementation
    public DoctorPerformanceReportDto generateDoctorPerformanceReport(Long doctorId, LocalDate startDate, LocalDate endDate) {
        DoctorPerformanceReportDto report = new DoctorPerformanceReportDto();

        try{
            // Get doctor details
            DoctorProfileDto doctor =  doctorServiceClient.getDoctorDetails(doctorId).getBody().getData();
            report.setDoctorName(doctor.getFullName());
            report.setSpecialization(doctor.getPrimarySpecialization());

            // Performance metrics
            Map<String, Object> performance = doctorServiceClient.getDoctorPerformance
                    (doctorId, startDate, endDate).getBody().getData();

            report.setTotalConsultations((Integer) performance.get("totalConsultations"));
            report.setCompletedConsultations((Integer) performance.get("completedConsultations"));
            report.setCancelledAppointments((Integer) performance.get("cancelledAppointments"));
            report.setAverageRating((Double) performance.get("averageRating"));
            report.setTotalRevenue((Double) performance.get("totalRevenue"));
            report.setAverageConsultationTime((Integer) performance.get("averageConsultationTime"));
            report.setPatientSatisfactionScore((Double) performance.get("satisfactionScore"));
            report.setResponseTime((Double) performance.get("averageResponseTime"));

            // Cases breakdown
            report.setCasesByCategory((Map<String, Integer>) performance.get("casesByCategory"));
            report.setCasesByUrgency((Map<String, Integer>) performance.get("casesByUrgency"));

            // Monthly trend
            report.setMonthlyTrend((List<MonthlyPerformanceDto>) performance.get("monthlyTrend"));
        } catch (Exception e) {
            log.error("Failed to get doctors with pending performance");
            log.error(e.getMessage());
        }



        return report;
    }

    // 31. Enable/Disable User Account Implementation
    @Transactional
    public void disableUserAccount(Long userId, String reason) {
        authServiceClient.updateUserStatus(userId, "SUSPENDED", reason);
        log.info("User account {} disabled. Reason: {}", userId, reason);
    }

    @Transactional
    public void enableUserAccount(Long userId) {
        authServiceClient.updateUserStatus(userId, "ACTIVE", "Account re-enabled by admin");
        log.info("User account {} enabled", userId);
    }

    // 32. Reset Password Implementation
    public ResetPasswordResponseDto resetUserPassword(Long userId) {
        String temporaryPassword = generateTemporaryPassword();
        //TODO remove this log.
        log.info("Reset password for user {}, temp password: {}", userId, temporaryPassword);

        authServiceClient.resetPassword(userId, temporaryPassword);

        ResetPasswordResponseDto response = new ResetPasswordResponseDto();
        response.setUserId(userId);
        response.setTemporaryPassword(temporaryPassword);
        response.setMessage("Password reset successfully. Temporary password sent to user's email.");

        return response;
    }

    // 33. Update System Configuration Implementation
    @Transactional
    public SystemConfig updateSystemConfig(SystemConfigDto dto) {
        SystemConfig config = systemConfigRepository.findByConfigKey(dto.getConfigKey())
                .orElse(new SystemConfig());

        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        config.setConfigType(dto.getConfigType());
        config.setDescription(dto.getDescription());

        return systemConfigRepository.save(config);
    }

    // 34. Manage Specializations Implementation
    @Transactional
    public void manageSpecializations(SpecializationsDto dto) {
        SystemConfig config = systemConfigRepository.findByConfigKey("SPECIALIZATIONS")
                .orElse(new SystemConfig());

        config.setConfigKey("SPECIALIZATIONS");
        config.setConfigValue(String.join(",", dto.getSpecializations()));
        config.setConfigType("LIST");
        config.setDescription("Medical specializations available in the system");

        systemConfigRepository.save(config);
    }

    // 35. Update Static Content Implementation
    @Transactional
    public void updateStaticContent(StaticContentDto dto) {
        staticContentRepository.updateContent(dto.getPage(), dto.getContent());
    }

    // 37. System Usage Analytics Implementation
    public SystemUsageAnalyticsDto getSystemUsageAnalytics(String period) {
        SystemUsageAnalyticsDto analytics = new SystemUsageAnalyticsDto();

        analytics.setPeriod(period);

        // User activity
        analytics.setDailyActiveUsers(calculateDailyActiveUsers(period));
        analytics.setMonthlyActiveUsers(calculateMonthlyActiveUsers());
        analytics.setPeakUsageHours(calculatePeakUsageHours());

        // Feature usage
        analytics.setFeatureUsage(calculateFeatureUsage());
        analytics.setMostUsedFeatures(getMostUsedFeatures());

        // Case analytics
        analytics.setCaseSubmissionRate(calculateCaseSubmissionRate(period));
        analytics.setAverageTimeToAssignment(calculateAverageTimeToAssignment());
        analytics.setSpecializationDemand(calculateSpecializationDemand());

        // Payment analytics
        analytics.setSubscriptionConversionRate(calculateSubscriptionConversionRate());
        analytics.setPaymentSuccessRate(calculatePaymentSuccessRate());
        analytics.setAverageRevenuePerUser(calculateARPU());

        // System performance
        analytics.setApiResponseTime(calculateAverageApiResponseTime());
        analytics.setErrorRate(calculateErrorRate());
        analytics.setSystemAvailability(calculateSystemAvailability());

        return analytics;
    }


    // Payment-related methods
    public List<PaymentRecordDto> getAllPaymentRecords(LocalDate startDate, LocalDate endDate) {
        return paymentServiceClient.getAllPayments(startDate, endDate).getBody().getData();
    }

    public List<SubscriptionPaymentDto> getSubscriptionPayments() {
        return paymentServiceClient.getSubscriptionPayments();
    }

    public List<ConsultationPaymentDto> getConsultationPayments() {
        return paymentServiceClient.getConsultationPayments();
    }

    @Transactional
    public void processRefund(RefundRequestDto dto) {
        paymentServiceClient.processRefund(dto.getPaymentId(), dto.getRefundAmount(), dto.getReason());
    }

//    // Helper methods
//    private UserDto mapToUserDto(UserDetailsNew user) {
//        UserDto dto = new UserDto();
//        dto.setId(user.getId());
//        dto.setEmail(user.getEmail());
//        dto.setRole(user.getRole());
//        dto.setStatus(user.getStatus());
//        dto.setCreatedAt(user.getCreatedAt());
//        dto.setLastLogin(user.getLastLogin());
//        return dto;
//    }

    private String generateTemporaryPassword() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8) + "!";
    }

    private double calculateSystemUptime() {
        // Calculate system uptime percentage
        return 99.9; // Placeholder
    }

    private int getActiveServicesCount() {
        // Get count of active microservices
        return 9; // All services
    }

    private double calculateErrorRate() {
        // Calculate error rate from logs
        return 0.1; // 0.1% error rate
    }

    private Map<String, Integer> calculateDailyActiveUsers(String period) {
        // Calculate DAU based on period
        return new HashMap<>();
    }

    private int calculateMonthlyActiveUsers() {
        //return userRepository.countActiveUsersThisMonth();
        return 1500;
    }

    private Map<Integer, Integer> calculatePeakUsageHours() {
        // Calculate peak usage hours
        return new HashMap<>();
    }

    private Map<String, Integer> calculateFeatureUsage() {
        // Calculate feature usage statistics
        return new HashMap<>();
    }

    private List<String> getMostUsedFeatures() {
        return Arrays.asList("Case Submission", "Appointment Scheduling", "Payment Processing");
    }

    private double calculateCaseSubmissionRate(String period) {
        // Calculate case submission rate
        return 0.0;
    }

    private double calculateAverageTimeToAssignment() {
        // Calculate average time from case submission to doctor assignment
        return 2.5; // hours
    }

    private Map<String, Integer> calculateSpecializationDemand() {
        // Calculate demand for each specialization
        return new HashMap<>();
    }

    private double calculateSubscriptionConversionRate() {
        // Calculate conversion rate from registration to subscription
        return 75.0; // 75%
    }

    private double calculatePaymentSuccessRate() {
        // Calculate payment success rate
        return 98.5; // 98.5%
    }

    private double calculateARPU() {
        // Calculate average revenue per user
        return 89.99;
    }

    private double calculateAverageApiResponseTime() {
        // Calculate average API response time
        return 250.0; // milliseconds
    }

    private double calculateSystemAvailability() {
        // Calculate system availability
        return 99.95; // 99.95%
    }

    public void notifyAdminOfNewDoctor(Long userId, String email) {
        try{
            adminEventProducer.sendDoctorRegistrationNotification(userId, email, 0L);
            log.info("New doctor registration requires admin review: {}", email);
        } catch (Exception e) {
            log.error("Failed to send notification about new doctor registration", e);
        }
    }

    public void updatePaymentStats(String paymentType, Double amount) {
        // Update payment and revenue statistics
        log.info("Updating payment stats: type={}, amount={}", paymentType, amount);
        // Implement financial statistics logic
    }

    /**
     * Suspend a user account
     */
    @Transactional
    public void suspendUser(Long userId, String reason) {
        try {
            // Update user status to SUSPENDED via auth service
            authServiceClient.updateUserStatus(userId, "SUSPENDED", reason);

            log.info("User account {} suspended. Reason: {}", userId, reason);

            // TODO: Send notification to user about suspension
            // You can add Kafka event publishing here if needed

        } catch (Exception e) {
            log.error("Failed to suspend user {}: {}", userId, e.getMessage());
            throw new BusinessException("Failed to suspend user account", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Unsuspend (Activate) a user account
     */
    @Transactional
    public void unsuspendUser(Long userId) {
        try {
            // Update user status to ACTIVE via auth service
            authServiceClient.updateUserStatus(userId, "ACTIVE", "Account re-enabled by admin");

            log.info("User account {} activated", userId);

            // TODO: Send notification to user about activation
            // You can add Kafka event publishing here if needed

        } catch (Exception e) {
            log.error("Failed to activate user {}: {}", userId, e.getMessage());
            throw new BusinessException("Failed to activate user account", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a user account permanently
     */
    @Transactional
    public void deleteUser(Long userId) {
        try {
            // Call auth service to delete the user
            authServiceClient.deleteUser(userId);

            log.info("User account {} deleted", userId);

            // TODO: You may want to add cleanup logic here:
            // - Delete related data in other microservices via Kafka events
            // - Archive user data for compliance
            // - Send final notification to user email

        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", userId, e.getMessage());
            throw new BusinessException("Failed to delete user account", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get user details by ID
     */
    public UserDto getUserById(Long userId) {
        try {
            // Call auth service to get user details
            ResponseEntity<ApiResponse<UserDto>> response = authServiceClient.getUserById(userId);
            return response.getBody().getData();
        } catch (Exception e) {
            log.error("Failed to get user {}: {}", userId, e.getMessage());
            throw new BusinessException("User not found", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Verify Doctor - Approve or Reject
     * @param doctorId The ID of the doctor to verify
     * @param verificationData Contains approved status, reason, and notes
     * @return Verification response with status and message
     */
    public DoctorVerificationResponseDto verifyDoctor(Long doctorId, VerifyDoctorRequestDto verificationData) {
        try {
            log.info("Admin verifying doctor ID: {} - Approved: {}", doctorId, verificationData.getApproved());

            // Validate request
            if (!verificationData.getApproved() &&
                    (verificationData.getReason() == null || verificationData.getReason().trim().isEmpty())) {
                throw new BusinessException(
                        "Rejection reason is required when rejecting a doctor",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Call doctor-service via Feign
            ResponseEntity<ApiResponse<DoctorVerificationResponseDto>> response =
                    doctorServiceClient.verifyDoctor(doctorId, verificationData);

            if (response.getBody() != null && response.getBody().getData() != null) {
                DoctorVerificationResponseDto result = response.getBody().getData();

                // Log the action
                log.info("Doctor {} verification {} by admin",
                        doctorId,
                        verificationData.getApproved() ? "APPROVED" : "REJECTED");

                publishDoctorVerificationNotification(result, verificationData.getApproved(), verificationData.getReason());

                return result;
            } else {
                log.error("Empty response from doctor-service for doctor ID: {}", doctorId);
                throw new BusinessException(
                        "Failed to verify doctor: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying doctor ID {}: {}", doctorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to verify doctor: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Reject Doctor Verification
     * Convenience method specifically for rejections
     */
    public DoctorVerificationResponseDto rejectDoctorVerification(Long doctorId, String reason) {
        try {
            log.info("Admin rejecting doctor ID: {} with reason: {}", doctorId, reason);

            if (reason == null || reason.trim().isEmpty()) {
                throw new BusinessException(
                        "Rejection reason is required",
                        HttpStatus.BAD_REQUEST
                );
            }

            RejectDoctorRequestDto request = RejectDoctorRequestDto.builder()
                    .reason(reason)
                    .build();

            ResponseEntity<ApiResponse<DoctorVerificationResponseDto>> response =
                    doctorServiceClient.rejectDoctor(doctorId, request);

            if (response.getBody() != null && response.getBody().getData() != null) {
                DoctorVerificationResponseDto result = response.getBody().getData();

                publishDoctorVerificationNotification(result, false, reason);
                log.info("Doctor {} rejected successfully", doctorId);
                return result;
            } else {
                throw new BusinessException(
                        "Failed to reject doctor: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error rejecting doctor ID {}: {}", doctorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to reject doctor: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Update Doctor Status (Active/Inactive/Suspended)
     * @param doctorId The ID of the doctor
     * @param status The new status (ACTIVE, INACTIVE, SUSPENDED)
     */
    public void updateDoctorStatus(Long doctorId, String status) {
        try {
            log.info("Admin updating doctor ID {} status to: {}", doctorId, status);

            // Validate status
            if (!status.matches("(?i)ACTIVE|INACTIVE|SUSPENDED")) {
                throw new BusinessException(
                        "Invalid status. Valid values: ACTIVE, INACTIVE, SUSPENDED",
                        HttpStatus.BAD_REQUEST
                );
            }

            UpdateDoctorStatusRequestDto request = UpdateDoctorStatusRequestDto.builder()
                    .status(status.toUpperCase())
                    .build();

            ResponseEntity<ApiResponse<Void>> response =
                    doctorServiceClient.updateDoctorStatus(doctorId, request);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Doctor {} status updated to {} successfully", doctorId, status);


            } else {
                throw new BusinessException(
                        "Failed to update doctor status",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating doctor ID {} status: {}", doctorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to update doctor status: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get All Doctors with Filters and Pagination
     * @param filters Filter criteria for doctors
     * @return Paginated list of doctor summaries
     */
    public Page<DoctorSummaryDto> getAllDoctors(DoctorFilterDto filters) {
        try {
            log.info("Fetching all doctors with filters: verificationStatus={}, specialization={}, searchTerm={}",
                    filters.getVerificationStatus(),
                    filters.getSpecialization(),
                    filters.getSearchTerm());

            ResponseEntity<ApiResponse<Page<DoctorSummaryDto>>> response =
                    doctorServiceClient.getAllDoctors(
                            filters.getSearchTerm(),
                            filters.getVerificationStatus(),
                            filters.getSpecialization(),
                            filters.getIsAvailable(),
                            filters.getEmergencyMode(),
                            filters.getMinYearsExperience(),
                            filters.getMaxYearsExperience(),
                            filters.getMinRating(),
                            filters.getCity(),
                            filters.getCountry(),
                            filters.getPage(),
                            filters.getSize(),
                            filters.getSortBy(),
                            filters.getSortDirection()
                    );

            if (response.getBody() != null && response.getBody().getData() != null) {
                Page<DoctorSummaryDto> doctors = response.getBody().getData();
                log.info("Retrieved {} doctors (page {} of {})",
                        doctors.getNumberOfElements(),
                        doctors.getNumber() + 1,
                        doctors.getTotalPages());
                return doctors;
            } else {
                log.warn("Empty response when fetching doctors");
                return Page.empty();
            }

        } catch (Exception e) {
            log.error("Error fetching doctors: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Failed to fetch doctors: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void publishDoctorVerificationNotification(
            DoctorVerificationResponseDto result,
            Boolean approved, String reason) {

        // Example notification event
    /*
    NotificationEvent event = NotificationEvent.builder()
        .recipientId(result.getDoctorId())
        .recipientType("DOCTOR")
        .type(approved ? "VERIFICATION_APPROVED" : "VERIFICATION_REJECTED")
        .title(approved ? "Verification Approved" : "Verification Rejected")
        .message(approved
            ? "Congratulations! Your doctor profile has been verified."
            : "Your doctor verification was not approved. Please contact support.")
        .build();

    notificationProducer.sendNotification(event);
    */

        log.info("Send notification to doctor {} about verification {}",
                result.getDoctorId(),
                approved ? "approval" : "rejection");

        adminEventProducer.sendDoctorStatusChangeNotification( result.getDoctorId(), result.getDoctorEmail(),
                result.getFullName(), result.getStatus(), reason, approved);
    }
}
