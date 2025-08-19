package com.adminservice.service;

import com.adminservice.dto.*;
import com.adminservice.entity.*;
import com.adminservice.feign.*;
import com.adminservice.repository.ComplaintRepository;
import com.adminservice.repository.SystemConfigRepository;
import com.adminservice.repository.StaticContentRepository;
import com.adminservice.repository.UserRepository;
import com.doctorservice.entity.Doctor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.print.Doc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        Map<String,Long> metricsMap = new HashMap<>();
        try{
            metricsMap = patientServiceClient.getAllMetrics().getBody().getData();
            metrics.setTotalCases(metricsMap.get("totalCasesCount"));
            metrics.setCasesInProgress(metricsMap.get("inProgressCasesCunt"));
            metrics.setAverageCaseResolutionTime(Double.valueOf(metricsMap.get("averageCaseResolutionTime").toString()));
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
            pendingVerifications= doctorServiceClient.getPendingVerifications().getBody().getData().
                    stream().map(this::convertToPendingVerificationDto).collect(Collectors.toList());
        }
        catch(Exception e){
            log.error("Failed to get doctors with pending verification");
            log.error(e.getMessage());
        }
        return pendingVerifications;
    }

    public PendingVerificationDto convertToPendingVerificationDto(Doctor doctor) {
        PendingVerificationDto pendingVerificationDto = new PendingVerificationDto();
        pendingVerificationDto.setDoctorId(doctor.getUserId());
        pendingVerificationDto.setFullName(doctor.getFullName());
        pendingVerificationDto.setLicenseNumber(doctor.getLicenseNumber());
        pendingVerificationDto.setSpecialization(doctor.getPrimarySpecializationCode());
        pendingVerificationDto.setSubmittedAt(doctor.getCreatedAt());
        pendingVerificationDto.setDocumentsUrl("To be changed .. added by me :)");
         return pendingVerificationDto;
    }

    // 29. Get All Users Implementation
    public Page<UserDto> getAllUsers(int page, int size, String role, String status) {
        Pageable pageable = PageRequest.of(page, size);

        if (role != null && status != null) {
            return userRepository.findByRoleAndStatus(role, status, pageable)
                    .map(this::mapToUserDto);
        } else if (role != null) {
            return userRepository.findByRole(role, pageable)
                    .map(this::mapToUserDto);
        } else if (status != null) {
            return userRepository.findByStatus(status, pageable)
                    .map(this::mapToUserDto);
        } else {
            return userRepository.findAll(pageable)
                    .map(this::mapToUserDto);
        }
    }

    public DoctorDetailsDto convertToDoctorDetailsDto(Doctor doctor){
        DoctorDetailsDto doctorDetailsDto = new DoctorDetailsDto();
        doctorDetailsDto.setId(doctor.getId());
        doctorDetailsDto.setFullName(doctor.getFullName());
        doctorDetailsDto.setLicenseNumber(doctor.getLicenseNumber());
        doctorDetailsDto.setPrimarySpecialization(doctor.getPrimarySpecializationCode());
        doctorDetailsDto.setSubSpecialization("Sub Specialization .. Added by me");
        doctorDetailsDto.setAverageRating(doctor.getAverageRating());
        doctorDetailsDto.setConsultationCount(doctor.getTotalConsultations());
        return doctorDetailsDto;
    }

    // 30. Generate Doctor Performance Report Implementation
    public DoctorPerformanceReportDto generateDoctorPerformanceReport(Long doctorId, LocalDate startDate, LocalDate endDate) {
        DoctorPerformanceReportDto report = new DoctorPerformanceReportDto();

        try{
            // Get doctor details
            DoctorDetailsDto doctor =  convertToDoctorDetailsDto(doctorServiceClient.
                    getDoctorDetails(doctorId).getBody().getData());
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

    // 36. Get All Complaints Implementation
    public List<Complaint> getAllComplaints(String status, String priority) {
        if (status != null && priority != null) {
            return complaintRepository.findByStatusAndPriority(
                    ComplaintStatus.valueOf(status),
                    ComplaintPriority.valueOf(priority));
        } else if (status != null) {
            return complaintRepository.findByStatus(ComplaintStatus.valueOf(status));
        } else if (priority != null) {
            return complaintRepository.findByPriority(ComplaintPriority.valueOf(priority));
        } else {
            return complaintRepository.findAll();
        }
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

    // 38. Respond to Complaint Implementation
    @Transactional
    public void respondToComplaint(Long complaintId, ComplaintResponseDto dto) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setAdminResponse(dto.getResponse());
        complaint.setStatus(ComplaintStatus.valueOf(dto.getStatus()));
        complaint.setResolvedAt(LocalDateTime.now());

        complaintRepository.save(complaint);

        // Notify the patient
        notificationServiceClient.sendNotification(
                0L, // System notification
                complaint.getPatientId(),
                "Complaint Response",
                "Your complaint has been reviewed. Status: " + dto.getStatus()
        );
    }

    // Payment-related methods
    public List<PaymentRecordDto> getAllPaymentRecords(LocalDate startDate, LocalDate endDate) {
        return paymentServiceClient.getAllPayments(startDate, endDate);
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

    // Helper methods
    private UserDto mapToUserDto(UserDetailsNew user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }

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
}
