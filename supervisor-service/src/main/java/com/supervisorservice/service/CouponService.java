package com.supervisorservice.service;

import com.commonlibrary.entity.CouponStatus;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.dto.*;
import com.supervisorservice.entity.CouponBatch;
import com.supervisorservice.entity.CouponRedemption;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorCoupon;
import com.supervisorservice.exception.ResourceNotFoundException;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.*;
import com.supervisorservice.util.CouponCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing coupons
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {
    
    private final SupervisorCouponRepository couponRepository;
    private final CouponBatchRepository batchRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorValidationService validationService;
    private final SupervisorKafkaProducer kafkaProducer;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final SupervisorKafkaProducer eventProducer;
    
    @Value("${supervisor.coupon-default-expiry-months:6}")
    private Integer defaultExpiryMonths;
    
    @Value("${supervisor.coupon-expiry-warning-days:30}")
    private Integer expiryWarningDays;
    
    @Value("${platform.consultation-fee:100.00}")
    private BigDecimal platformConsultationFee;
    
    /**
     * Issue a single coupon (Admin only)
     */
    @Transactional
    public CouponDto issueCoupon(IssueCouponRequest request, Long issuedByUserId) {
        log.info("Issuing coupon - supervisorId: {}, patientId: {}", 
                request.getSupervisorId(), request.getPatientId());
        
        // Validate supervisor exists
        MedicalSupervisor supervisor = validationService.validateSupervisorExists(request.getSupervisorId());
        
        // Generate unique coupon code
        String couponCode = generateUniqueCouponCode();
        
        // Determine expiry date
        Integer expiryMonths = request.getExpiryMonths() != null 
                ? request.getExpiryMonths() 
                : defaultExpiryMonths;
        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(expiryMonths);
        
        // Create coupon
        SupervisorCoupon coupon = SupervisorCoupon.builder()
                .couponCode(couponCode)
                .supervisor(supervisor)
                .patientId(request.getPatientId())
                .amount(request.getAmount())
                .currency("USD")
                .status(CouponStatus.AVAILABLE)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .issuedBy(issuedByUserId)
                .notes(request.getNotes())
                .isDeleted(false)
                .build();
        
        coupon = couponRepository.save(coupon);
        log.info("Coupon issued successfully: {}", couponCode);
        
        // Publish event
        kafkaProducer.sendCouponIssuedEvent(coupon);
        
        return mapToDto(coupon);
    }
    
    /**
     * Issue a batch of coupons (Admin only)
     */
    @Transactional
    public List<CouponDto> issueCouponBatch(IssueCouponBatchRequest request, Long issuedByUserId) {
        log.info("Issuing coupon batch - supervisorId: {}, patientId: {}, count: {}", 
                request.getSupervisorId(), request.getPatientId(), request.getTotalCoupons());
        
        // Validate supervisor exists
        MedicalSupervisor supervisor = validationService.validateSupervisorExists(request.getSupervisorId());
        
        // Create batch record
        String batchCode = CouponCodeGenerator.generateBatchCode();
        Integer expiryMonths = request.getExpiryMonths() != null 
                ? request.getExpiryMonths() 
                : defaultExpiryMonths;
        
        CouponBatch batch = CouponBatch.builder()
                .batchCode(batchCode)
                .supervisor(supervisor)
                .patientId(request.getPatientId())
                .totalCoupons(request.getTotalCoupons())
                .amountPerCoupon(request.getAmountPerCoupon())
                .expiryMonths(expiryMonths)
                .issuedBy(issuedByUserId)
                .notes(request.getNotes())
                .build();
        
        batch = batchRepository.save(batch);
        
        // Create individual coupons
        List<SupervisorCoupon> coupons = new ArrayList<>();
        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(expiryMonths);
        
        for (int i = 0; i < request.getTotalCoupons(); i++) {
            String couponCode = generateUniqueCouponCode();
            
            SupervisorCoupon coupon = SupervisorCoupon.builder()
                    .couponCode(couponCode)
                    .supervisor(supervisor)
                    .patientId(request.getPatientId())
                    .amount(request.getAmountPerCoupon())
                    .currency("USD")
                    .status(CouponStatus.AVAILABLE)
                    .issuedAt(LocalDateTime.now())
                    .expiresAt(expiresAt)
                    .issuedBy(issuedByUserId)
                    .batch(batch)
                    .notes(request.getNotes())
                    .isDeleted(false)
                    .build();
            
            coupons.add(coupon);
        }
        
        coupons = couponRepository.saveAll(coupons);
        log.info("Coupon batch issued successfully: {} - {} coupons", batchCode, coupons.size());
        
        // Publish events
        coupons.forEach(kafkaProducer::sendCouponIssuedEvent);
        
        return coupons.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Redeem coupon for case payment
     */
    @Transactional
    public PaymentResultDto redeemCoupon(Long userId, Long caseId, String couponCode, Long patientId) {
        log.info("Redeeming coupon: {} for caseId: {}", couponCode, caseId);
        
        // Validate supervisor
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        
        // Validate patient access
        validationService.validatePatientAccess(supervisor.getId(), patientId);
        
        // Validate and get coupon
        SupervisorCoupon coupon = validationService.validateCouponUsable(couponCode, patientId);
        
        // Mark coupon as used
        coupon.markAsUsed(caseId);
        coupon = couponRepository.save(coupon);
        
        // Create redemption record (payment ID will be updated by payment service)
        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .paymentId(0L) // Placeholder, will be updated by payment service
                .caseId(caseId)
                .patientId(patientId)
                .supervisor(supervisor)
                .amount(coupon.getAmount())
                .build();
        
        redemption = redemptionRepository.save(redemption);
        log.info("Coupon redeemed successfully: {}", couponCode);
        
        // Publish event
        kafkaProducer.sendCouponRedeemedEvent(coupon, caseId, redemption.getPaymentId());
        
        // Return payment result
        return PaymentResultDto.builder()
                .paymentId(redemption.getPaymentId())
                .caseId(caseId)
                .patientId(patientId)
                .supervisorId(supervisor.getId())
                .paymentSource("COUPON")
                .amount(coupon.getAmount())
                .currency(coupon.getCurrency())
                .status("COMPLETED")
                .couponId(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .timestamp(LocalDateTime.now())
                .message("Payment completed successfully using coupon")
                .build();
    }
    
    /**
     * Get available coupons for a patient
     */
    @Transactional(readOnly = true)
    public List<CouponDto> getAvailableCoupons(Long userId, Long patientId) {
        log.debug("Getting available coupons for patient: {}", patientId);
        
        // Validate supervisor and patient access
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        validationService.validatePatientAccess(supervisor.getId(), patientId);
        
        List<SupervisorCoupon> coupons = couponRepository.findAvailableBySupervisorAndPatient(
                supervisor.getId(), patientId, LocalDateTime.now());
        
        return coupons.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get coupon summary for supervisor
     */
    @Transactional(readOnly = true)
    public CouponSummaryDto getCouponSummary(Long userId) {
        log.debug("Getting coupon summary for userId: {}", userId);
        
        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);
        
//        Object[] stats = couponRepository.getCouponStatistics(supervisor.getId(), LocalDateTime.now());
//
//        Long available = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
//        Long used = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
//        Long expired = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
//        Long cancelled = stats[3] != null ? ((Number) stats[3]).longValue() : 0L;



        SupervisorCouponRepository.CouponStatistics stats = couponRepository.getCouponStatistics(supervisor.getId(), LocalDateTime.now());

        Long available = stats.getAvailable() != null ? stats.getAvailable() : 0L;
        Long used = stats.getUsed() != null ? stats.getUsed() : 0L;
        Long expired = stats.getExpired() != null ? stats.getExpired() : 0L;
        Long cancelled = stats.getCancelled() != null ? stats.getCancelled() : 0L;

        
        Double availableValue = couponRepository.getTotalAvailableValueBySupervisor(
                supervisor.getId(), LocalDateTime.now());
        
        // Get coupons expiring soon
        LocalDateTime warningDate = LocalDateTime.now().plusDays(expiryWarningDays);
        List<SupervisorCoupon> expiringSoon = couponRepository.findCouponsExpiringSoon(
                LocalDateTime.now(), warningDate);
        
        return CouponSummaryDto.builder()
                .supervisorId(supervisor.getId())
                .totalCoupons(available.intValue() + used.intValue() + expired.intValue() + cancelled.intValue())
                .availableCoupons(available.intValue())
                .usedCoupons(used.intValue())
                .expiredCoupons(expired.intValue())
                .cancelledCoupons(cancelled.intValue())
                .couponsExpiringSoon(expiringSoon.size())
                .totalAvailableValue(BigDecimal.valueOf(availableValue != null ? availableValue : 0.0))
                .build();
    }
    
    /**
     * Cancel a coupon (Admin only)
     */
    @Transactional
    public void cancelCoupon(Long couponId, String reason) {
        log.info("Cancelling coupon: {} - Reason: {}", couponId, reason);
        
        SupervisorCoupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id: " + couponId));
        
        coupon.cancel(reason);
        couponRepository.save(coupon);
        
        // Publish event
        kafkaProducer.sendCouponCancelledEvent(coupon, reason);
        
        log.info("Coupon cancelled successfully: {}", coupon.getCouponCode());
    }
    
    /**
     * Scheduled task: Expire old coupons
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void expireOldCoupons() {
        log.info("Running scheduled task: Expire old coupons");
        
        List<SupervisorCoupon> expiredCoupons = couponRepository.findExpiredAvailableCoupons(LocalDateTime.now());
        
        if (expiredCoupons.isEmpty()) {
            log.info("No coupons to expire");
            return;
        }
        
        int count = 0;
        for (SupervisorCoupon coupon : expiredCoupons) {
            coupon.markAsExpired();
            couponRepository.save(coupon);
            kafkaProducer.sendCouponExpiredEvent(coupon);
            count++;
        }
        
        log.info("Expired {} coupons", count);
    }
    
    /**
     * Scheduled task: Send expiry warnings
     * Runs daily at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendExpiryWarnings() {
        log.info("Running scheduled task: Send expiry warnings");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningDate = now.plusDays(expiryWarningDays);
        
        List<SupervisorCoupon> couponsExpiringSoon = couponRepository.findCouponsExpiringSoon(now, warningDate);
        
        if (couponsExpiringSoon.isEmpty()) {
            log.info("No coupons expiring soon");
            return;
        }
        
        for (SupervisorCoupon coupon : couponsExpiringSoon) {
            int daysUntilExpiry = (int) ChronoUnit.DAYS.between(now, coupon.getExpiresAt());
            kafkaProducer.sendCouponExpiringSoonEvent(coupon, daysUntilExpiry);
        }
        
        log.info("Sent {} expiry warning notifications", couponsExpiringSoon.size());
    }
    
    /**
     * Generate unique coupon code
     */
    private String generateUniqueCouponCode() {
        String code;
        int attempts = 0;
        do {
            code = CouponCodeGenerator.generate();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Failed to generate unique coupon code after 10 attempts");
            }
        } while (couponRepository.existsByCouponCode(code));
        
        return code;
    }
    
    /**
     * Map entity to DTO
     */
    private CouponDto mapToDto(SupervisorCoupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        int daysUntilExpiry = (int) ChronoUnit.DAYS.between(now, coupon.getExpiresAt());
        boolean isExpiringSoon = coupon.isExpiringSoon(expiryWarningDays);
        
        return CouponDto.builder()
                .id(coupon.getId())
                .couponCode(coupon.getCouponCode())
                .supervisorId(coupon.getSupervisor().getId())
                .patientId(coupon.getPatientId())
                .amount(coupon.getAmount())
                .currency(coupon.getCurrency())
                .caseId(coupon.getCaseId())
                .status(coupon.getStatus())
                .issuedAt(coupon.getIssuedAt())
                .expiresAt(coupon.getExpiresAt())
                .usedAt(coupon.getUsedAt())
                .cancelledAt(coupon.getCancelledAt())
                .issuedBy(coupon.getIssuedBy())
                .batchId(coupon.getBatch() != null ? coupon.getBatch().getId() : null)
                .batchCode(coupon.getBatch() != null ? coupon.getBatch().getBatchCode() : null)
                .notes(coupon.getNotes())
                .cancellationReason(coupon.getCancellationReason())
                .isExpiringSoon(isExpiringSoon)
                .daysUntilExpiry(daysUntilExpiry > 0 ? daysUntilExpiry : 0)
                .supervisorFullName(coupon.getSupervisor().getFullName())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }




    /**
     * Validate a coupon for payment
     */
    public CouponValidationResponseDto validateCoupon(
            String couponCode,
            Long patientId,
            Long supervisorId,
            BigDecimal consultationFee) {

        log.info("Validating coupon {} for patient {} by supervisor {}",
                couponCode, patientId, supervisorId);

        // Find available coupon
        var couponOpt = couponRepository.findAvailableCoupon(
                couponCode, supervisorId, patientId, CouponStatus.AVAILABLE);

        if (couponOpt.isEmpty()) {
            log.warn("Coupon {} not found or not available for patient {}", couponCode, patientId);
            return CouponValidationResponseDto.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .message("Coupon not found, expired, or not available for this patient")
                    .build();
        }

        SupervisorCoupon coupon = couponOpt.get();

        // Check if expired
        if (coupon.isExpired()) {
            log.warn("Coupon {} has expired", couponCode);
            return CouponValidationResponseDto.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .couponId(coupon.getId())
                    .expiresAt(coupon.getExpiresAt())
                    .message("Coupon has expired on " + coupon.getExpiresAt())
                    .build();
        }

        // Check patient-specific coupon
        if (coupon.getPatientId() != null && !coupon.getPatientId().equals(patientId)) {
            log.warn("Coupon {} is assigned to different patient", couponCode);
            return CouponValidationResponseDto.builder()
                    .valid(false)
                    .couponCode(couponCode)
                    .couponId(coupon.getId())
                    .message("Coupon is not valid for this patient")
                    .build();
        }

        // Calculate discount
        BigDecimal discountAmount = calculateDiscountAmount(coupon, consultationFee);
        BigDecimal remainingAmount = consultationFee.subtract(discountAmount);

        // Ensure remaining is not negative
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        log.info("Coupon {} validated: discount={}, remaining={}",
                couponCode, discountAmount, remainingAmount);

        return CouponValidationResponseDto.builder()
                .valid(true)
                .couponId(coupon.getId())
                .couponCode(couponCode)
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .remainingAmount(remainingAmount)
                .originalAmount(consultationFee)
                .expiresAt(coupon.getExpiresAt())
                .patientId(coupon.getPatientId())
                .message("Coupon is valid" +
                        (remainingAmount.compareTo(BigDecimal.ZERO) > 0 ?
                                ". Remaining amount: $" + remainingAmount :
                                ". Full coverage applied"))
                .build();
    }

    /**
     * Redeem a coupon for case payment
     */
    @Transactional
    public CouponRedemptionDto redeemCoupon(
            String couponCode,
            Long caseId,
            Long patientId,
            Long supervisorId,
            BigDecimal consultationFee) {

        log.info("Redeeming coupon {} for case {} by supervisor {}",
                couponCode, caseId, supervisorId);

        // Find and validate coupon
        SupervisorCoupon coupon = couponRepository.findAvailableCoupon(
                        couponCode, supervisorId, patientId, CouponStatus.AVAILABLE)
                .orElseThrow(() -> new BusinessException(
                        "Coupon not found or not available", HttpStatus.BAD_REQUEST));

        // Calculate discount
        BigDecimal discountAmount = calculateDiscountAmount(coupon, consultationFee);
        BigDecimal finalAmount = consultationFee.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // Generate transaction ID
        String transactionId = "CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Update coupon status
        coupon.setStatus(CouponStatus.USED);
        coupon.setUsedAt(LocalDateTime.now());
        coupon.setUsedForCaseId(caseId);
        coupon.setTransactionId(transactionId);
        couponRepository.save(coupon);

        log.info("Coupon {} redeemed successfully for case {}, discount: {}",
                couponCode, caseId, discountAmount);

        // Send Kafka event
        eventProducer.sendCouponRedeemedEvent(
                coupon, caseId, coupon.getPaymentId());

        return CouponRedemptionDto.builder()
                .couponId(coupon.getId())
                .couponCode(couponCode)
                .caseId(caseId)
                .patientId(patientId)
                .supervisorId(supervisorId)
                .originalAmount(consultationFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .transactionId(transactionId)
                .redeemedAt(LocalDateTime.now())
                .message("Coupon redeemed successfully")
                .build();
    }

    /**
     * Get available coupons for a patient
     */
    public List<CouponDto> getAvailableCouponsForPatient(Long userId, Long patientId) {
        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);
        validationService.validatePatientAssignment(supervisor.getId(), patientId);

        List<SupervisorCoupon> coupons = couponRepository.findAvailableCouponsForPatient(
                supervisor.getId(), patientId);

        return coupons.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all available coupons for supervisor
     */
    public List<CouponDto> getAllAvailableCoupons(Long userId) {
        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);

        List<SupervisorCoupon> coupons = couponRepository.findAllAvailableCoupons(supervisor.getId());

        return coupons.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get coupon summary for supervisor
     */
//    public Map<String, Object> getCouponSummary(Long userId) {
//        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);
//        Long supervisorId = supervisor.getId();
//
//        Map<String, Object> summary = new HashMap<>();
//
//        long totalCoupons = couponRepository.countBySupervisorIdAndStatusAndIsDeletedFalse(
//                supervisorId, CouponStatus.AVAILABLE) +
//                couponRepository.countBySupervisorIdAndStatusAndIsDeletedFalse(
//                        supervisorId, CouponStatus.USED) +
//                couponRepository.countBySupervisorIdAndStatusAndIsDeletedFalse(
//                        supervisorId, CouponStatus.EXPIRED);
//
//        long availableCoupons = couponRepository.countAvailableCoupons(supervisorId);
//        long usedCoupons = couponRepository.countBySupervisorIdAndStatusAndIsDeletedFalse(
//                supervisorId, CouponStatus.USED);
//        long expiredCoupons = couponRepository.countBySupervisorIdAndStatusAndIsDeletedFalse(
//                supervisorId, CouponStatus.EXPIRED);
//
//        BigDecimal totalAvailableValue = couponRepository.getTotalAvailableValue(supervisorId);
//
//        summary.put("totalCoupons", totalCoupons);
//        summary.put("availableCoupons", availableCoupons);
//        summary.put("usedCoupons", usedCoupons);
//        summary.put("expiredCoupons", expiredCoupons);
//        summary.put("totalAvailableValue", totalAvailableValue);
//
//        return summary;
//    }

    /**
     * Get coupons expiring soon
     */
    public List<CouponDto> getCouponsExpiringSoon(Long userId, int days) {
        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);

        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        List<SupervisorCoupon> coupons = couponRepository.findCouponsExpiringSoon(
                supervisor.getId(), threshold);

        return coupons.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate discount amount based on coupon type
     */
    private BigDecimal calculateDiscountAmount(SupervisorCoupon coupon, BigDecimal consultationFee) {
        BigDecimal discountAmount;

        switch (coupon.getDiscountType()) {
            case PERCENTAGE:
                discountAmount = consultationFee
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Apply max discount cap if set
                if (coupon.getMaxDiscountAmount() != null &&
                        discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaxDiscountAmount();
                }
                break;

            case FIXED_AMOUNT:
                discountAmount = coupon.getDiscountValue();
                break;

            case FULL_COVERAGE:
                discountAmount = consultationFee;
                break;

            default:
                discountAmount = BigDecimal.ZERO;
        }

        // Cap discount at consultation fee
        if (discountAmount.compareTo(consultationFee) > 0) {
            discountAmount = consultationFee;
        }

        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }


    /**
     * Mark expired coupons (scheduled task)
     */
    @Transactional
    public int markExpiredCoupons() {
        int count = couponRepository.markExpiredCoupons();
        if (count > 0) {
            log.info("Marked {} coupons as expired", count);
        }
        return count;
    }
}
