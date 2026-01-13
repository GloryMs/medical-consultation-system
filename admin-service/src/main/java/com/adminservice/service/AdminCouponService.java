package com.adminservice.service;

import com.adminservice.entity.AdminCoupon;
import com.adminservice.entity.AdminCouponBatch;
import com.adminservice.entity.CouponRedemptionHistory;
import com.adminservice.kafka.CouponEventProducer;
import com.adminservice.mapper.AdminCouponMapper;
import com.adminservice.repository.AdminCouponBatchRepository;
import com.adminservice.repository.AdminCouponRepository;
import com.adminservice.repository.CouponRedemptionHistoryRepository;
import com.adminservice.util.CouponCodeGenerator;
import com.commonlibrary.constants.CouponErrorCodes;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.AdminCouponStatus;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.CouponBatchStatus;
import com.commonlibrary.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing coupons in admin-service.
 * This is the source of truth for all coupon operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminCouponService {

    private final AdminCouponRepository couponRepository;
    private final AdminCouponBatchRepository batchRepository;
    private final CouponRedemptionHistoryRepository redemptionRepository;
    private final AdminCouponMapper couponMapper;
    private final CouponCodeGenerator codeGenerator;
    private final CouponEventProducer eventProducer;

    // ==================== Coupon Creation ====================

    /**
     * Create a single coupon
     */
    public AdminCouponDto createCoupon(CreateCouponRequest request, Long adminUserId) {
        log.info("Admin {} creating coupon with discount type: {}", adminUserId, request.getDiscountType());

        // Generate coupon code if not provided
        String couponCode = request.getCouponCode();
        if (couponCode == null || couponCode.trim().isEmpty()) {
            couponCode = generateUniqueCouponCode();
        } else {
            couponCode = codeGenerator.normalizeCouponCode(couponCode);
            // Check if code already exists
            if (couponRepository.existsByCouponCode(couponCode)) {
                throw new BusinessException("Coupon code already exists: " + couponCode, HttpStatus.CONFLICT);
            }
        }

        // Create entity
        AdminCoupon coupon = couponMapper.toEntity(request, adminUserId);
        coupon.setCouponCode(couponCode);
        coupon.setStatus(AdminCouponStatus.CREATED);

        // Save coupon
        AdminCoupon savedCoupon = couponRepository.save(coupon);
        log.info("Coupon created: {} with ID: {}", couponCode, savedCoupon.getId());

        // Auto-distribute if requested and beneficiary is specified
        if (Boolean.TRUE.equals(request.getAutoDistribute()) && request.getBeneficiaryId() != null) {
            savedCoupon = distributeCouponInternal(savedCoupon, request.getBeneficiaryId(), adminUserId, null);
        }

        // Send Kafka event
        eventProducer.sendCouponCreatedEvent(savedCoupon);

        return couponMapper.toDto(savedCoupon);
    }

    /**
     * Create a batch of coupons
     */
    public CouponBatchDto createBatchCoupons(CreateBatchCouponsRequest request, Long adminUserId) {
        log.info("Admin {} creating batch of {} coupons", adminUserId, request.getTotalCoupons());

        // Generate batch code
        String batchCode = request.getBatchCodePrefix() != null
                ? codeGenerator.generateBatchCode(request.getBatchCodePrefix())
                : codeGenerator.generateBatchCode();

        // Check if batch code exists
        while (batchRepository.existsByBatchCode(batchCode)) {
            batchCode = codeGenerator.generateBatchCode();
        }

        // Create batch entity
        AdminCouponBatch batch = couponMapper.toBatchEntity(request, adminUserId);
        batch.setBatchCode(batchCode);
        batch.setStatus(CouponBatchStatus.CREATED);

        // Save batch
        AdminCouponBatch savedBatch = batchRepository.save(batch);
        log.info("Batch created: {} with ID: {}", batchCode, savedBatch.getId());

        // Create individual coupons
        List<AdminCoupon> coupons = new ArrayList<>();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(request.getExpiryDays());

        for (int i = 0; i < request.getTotalCoupons(); i++) {
            String couponCode = generateUniqueCouponCode();

            AdminCoupon coupon = AdminCoupon.builder()
                    .couponCode(couponCode)
                    .discountType(request.getDiscountType())
                    .discountValue(request.getDiscountValue())
                    .maxDiscountAmount(request.getMaxDiscountAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                    .beneficiaryType(request.getBeneficiaryType())
                    .beneficiaryId(request.getBeneficiaryId())
                    .status(AdminCouponStatus.CREATED)
                    .batchId(savedBatch.getId())
                    .expiresAt(expiresAt)
                    .isTransferable(request.getIsTransferable() != null ? request.getIsTransferable() : true)
                    .notes(request.getNotes())
                    .createdBy(adminUserId)
                    .isDeleted(false)
                    .build();

            coupons.add(coupon);
        }

        // Batch save coupons
        couponRepository.saveAll(coupons);
        log.info("Created {} coupons for batch: {}", coupons.size(), batchCode);

        // Auto-distribute if requested
        if (Boolean.TRUE.equals(request.getAutoDistribute()) && request.getBeneficiaryId() != null) {
            distributeBatchInternal(savedBatch.getId(), request.getBeneficiaryId(), adminUserId);
            savedBatch.setStatus(CouponBatchStatus.DISTRIBUTED);
            savedBatch.setDistributedBy(adminUserId);
            savedBatch.setDistributedAt(LocalDateTime.now());
            savedBatch = batchRepository.save(savedBatch);
        }

        // Send Kafka event
        eventProducer.sendBatchCreatedEvent(savedBatch, coupons.size());

        return couponMapper.toBatchDto(savedBatch);
    }

    // ==================== Coupon Distribution ====================

    /**
     * Distribute a single coupon to a beneficiary
     */
    public AdminCouponDto distributeCoupon(Long couponId, DistributeCouponRequest request, Long adminUserId) {
        log.info("Admin {} distributing coupon {} to {} {}", 
                adminUserId, couponId, request.getBeneficiaryType(), request.getBeneficiaryId());

        AdminCoupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("Coupon not found", HttpStatus.NOT_FOUND));

        // Validate coupon can be distributed
        if (coupon.getStatus() != AdminCouponStatus.CREATED) {
            throw new BusinessException(
                    "Coupon cannot be distributed. Current status: " + coupon.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        if (Boolean.TRUE.equals(coupon.getIsDeleted())) {
            throw new BusinessException("Coupon has been deleted", HttpStatus.BAD_REQUEST);
        }

        // Distribute
        AdminCoupon distributed = distributeCouponInternal(
                coupon, request.getBeneficiaryId(), adminUserId, request.getNotes());

        // Send notification if requested
        if (Boolean.TRUE.equals(request.getSendNotification())) {
            eventProducer.sendCouponDistributedNotification(distributed);
        }

        return couponMapper.toDto(distributed);
    }

    /**
     * Distribute an entire batch to a beneficiary
     */
    public CouponBatchDto distributeBatch(Long batchId, DistributeCouponRequest request, Long adminUserId) {
        log.info("Admin {} distributing batch {} to {} {}", 
                adminUserId, batchId, request.getBeneficiaryType(), request.getBeneficiaryId());

        AdminCouponBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("Batch not found", HttpStatus.NOT_FOUND));

        // Validate batch can be distributed
        if (batch.getStatus() != CouponBatchStatus.CREATED) {
            throw new BusinessException(
                    "Batch cannot be distributed. Current status: " + batch.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        // Distribute all coupons in batch
        int distributedCount = distributeBatchInternal(batchId, request.getBeneficiaryId(), adminUserId);

        // Update batch status
        batch.setStatus(CouponBatchStatus.DISTRIBUTED);
        batch.setBeneficiaryId(request.getBeneficiaryId());
        batch.setDistributedBy(adminUserId);
        batch.setDistributedAt(LocalDateTime.now());
        AdminCouponBatch savedBatch = batchRepository.save(batch);

        log.info("Distributed {} coupons from batch {}", distributedCount, batch.getBatchCode());

        // Send notification if requested
        if (Boolean.TRUE.equals(request.getSendNotification())) {
            eventProducer.sendBatchDistributedNotification(savedBatch, distributedCount);
        }

        return couponMapper.toBatchDto(savedBatch);
    }

    private AdminCoupon distributeCouponInternal(AdminCoupon coupon, Long beneficiaryId, 
                                                  Long adminUserId, String notes) {
        coupon.setBeneficiaryId(beneficiaryId);
        coupon.setStatus(AdminCouponStatus.DISTRIBUTED);
        coupon.setDistributedBy(adminUserId);
        coupon.setDistributedAt(LocalDateTime.now());
        if (notes != null) {
            coupon.setNotes(notes);
        }

        AdminCoupon saved = couponRepository.save(coupon);

        // Send Kafka event for supervisor/patient service
        eventProducer.sendCouponDistributedEvent(saved);

        return saved;
    }

    private int distributeBatchInternal(Long batchId, Long beneficiaryId, Long adminUserId) {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.distributeBatch(batchId, beneficiaryId, adminUserId, now);
    }

    // ==================== Coupon Validation (Called by Payment Service) ====================

    /**
     * Validate a coupon for redemption
     * This is called by payment-service before processing payment
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(CouponValidationRequest request) {
        log.info("Validating coupon {} for beneficiary {} {}", 
                request.getCouponCode(), request.getBeneficiaryType(), request.getBeneficiaryId());

        String couponCode = codeGenerator.normalizeCouponCode(request.getCouponCode());

        // Find coupon
        AdminCoupon coupon = couponRepository.findByCouponCodeAndIsDeletedFalse(couponCode)
                .orElse(null);

        if (coupon == null) {
            return couponMapper.toValidationResponse(null, request.getRequestedAmount(), 
                    false, "Coupon not found", CouponErrorCodes.COUPON_NOT_FOUND);
        }

        // Check status
        if (coupon.getStatus() != AdminCouponStatus.DISTRIBUTED) {
            String message = switch (coupon.getStatus()) {
                case CREATED -> "Coupon has not been distributed yet";
                case USED -> "Coupon has already been used";
                case EXPIRED -> "Coupon has expired";
                case CANCELLED -> "Coupon has been cancelled";
                case SUSPENDED -> "Coupon is suspended";
                default -> "Coupon is not available";
            };
            String errorCode = switch (coupon.getStatus()) {
                case USED -> CouponErrorCodes.COUPON_ALREADY_USED;
                case EXPIRED -> CouponErrorCodes.COUPON_EXPIRED;
                case CANCELLED -> CouponErrorCodes.COUPON_CANCELLED;
                default -> CouponErrorCodes.COUPON_NOT_DISTRIBUTED;
            };
            return couponMapper.toValidationResponse(coupon, request.getRequestedAmount(), 
                    false, message, errorCode);
        }

        // Check expiration
        if (coupon.isExpired()) {
            return couponMapper.toValidationResponse(coupon, request.getRequestedAmount(), 
                    false, "Coupon has expired", CouponErrorCodes.COUPON_EXPIRED);
        }

        // Check beneficiary
        if (!coupon.getBeneficiaryType().equals(request.getBeneficiaryType()) ||
            !coupon.getBeneficiaryId().equals(request.getBeneficiaryId())) {
            return couponMapper.toValidationResponse(coupon, request.getRequestedAmount(), 
                    false, "Coupon does not belong to this beneficiary", 
                    CouponErrorCodes.COUPON_BENEFICIARY_MISMATCH);
        }

        // Coupon is valid
        log.info("Coupon {} validated successfully", couponCode);
        return couponMapper.toValidationResponse(coupon, request.getRequestedAmount(), 
                true, "Coupon is valid", null);
    }

    // ==================== Mark Coupon as Used (Called by Payment Service) ====================

    /**
     * Mark a coupon as used after successful payment
     * This is called by payment-service after payment is processed
     */
    public MarkCouponUsedResponse markCouponAsUsed(String couponCode, MarkCouponUsedRequest request) {
        log.info("Marking coupon {} as used for case {} payment {}", 
                couponCode, request.getCaseId(), request.getPaymentId());

        couponCode = codeGenerator.normalizeCouponCode(couponCode);

        AdminCoupon coupon = couponRepository.findByCouponCodeAndIsDeletedFalse(couponCode)
                .orElseThrow(() -> new BusinessException("Coupon not found: ", HttpStatus.NOT_FOUND));

        // Validate coupon can be used
        if (coupon.getStatus() != AdminCouponStatus.DISTRIBUTED) {
            throw new BusinessException(
                    "Coupon cannot be marked as used. Current status: " + coupon.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        // Update coupon
        LocalDateTime usedAt = request.getUsedAt() != null ? request.getUsedAt() : LocalDateTime.now();
        coupon.setStatus(AdminCouponStatus.USED);
        coupon.setUsedAt(usedAt);
        coupon.setUsedForCaseId(request.getCaseId());
        coupon.setUsedForPaymentId(request.getPaymentId());
        coupon.setUsedByPatientId(request.getPatientId());

        AdminCoupon savedCoupon = couponRepository.save(coupon);

        // Create redemption history
        CouponRedemptionHistory history = couponMapper.toRedemptionHistory(
                coupon, request, coupon.getBeneficiaryType());
        redemptionRepository.save(history);

        // Update batch status if part of batch
        if (coupon.getBatchId() != null) {
            updateBatchStatusAfterUsage(coupon.getBatchId());
        }

        // Send Kafka event
        eventProducer.sendCouponUsedEvent(savedCoupon, request);

        log.info("Coupon {} marked as used successfully", couponCode);

        return MarkCouponUsedResponse.builder()
                .success(true)
                .couponId(savedCoupon.getId())
                .couponCode(savedCoupon.getCouponCode())
                .usedAt(usedAt)
                .message("Coupon marked as used successfully")
                .build();
    }

    private void updateBatchStatusAfterUsage(Long batchId) {
        long totalCoupons = couponRepository.findByBatchIdAndIsDeletedFalse(batchId).size();
        long usedCoupons = couponRepository.countByBatchIdAndStatus(batchId, AdminCouponStatus.USED);

        CouponBatchStatus newStatus;
        if (usedCoupons == totalCoupons) {
            newStatus = CouponBatchStatus.FULLY_USED;
        } else if (usedCoupons > 0) {
            newStatus = CouponBatchStatus.PARTIALLY_USED;
        } else {
            return; // No change needed
        }

        batchRepository.updateBatchStatus(batchId, newStatus, LocalDateTime.now());
    }

    // ==================== Coupon Cancellation ====================

    /**
     * Cancel a single coupon
     */
    public AdminCouponDto cancelCoupon(Long couponId, CancelCouponRequest request, Long adminUserId) {
        log.info("Admin {} cancelling coupon {}", adminUserId, couponId);

        AdminCoupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("Coupon not found", HttpStatus.NOT_FOUND));

        // Validate coupon can be cancelled
        if (coupon.getStatus() == AdminCouponStatus.USED) {
            throw new BusinessException("Cannot cancel a used coupon", HttpStatus.BAD_REQUEST);
        }

        if (coupon.getStatus() == AdminCouponStatus.CANCELLED) {
            throw new BusinessException("Coupon is already cancelled", HttpStatus.BAD_REQUEST);
        }

        // Cancel coupon
        LocalDateTime now = LocalDateTime.now();
        coupon.setStatus(AdminCouponStatus.CANCELLED);
        coupon.setCancellationReason(request.getReason());
        coupon.setCancelledBy(adminUserId);
        coupon.setCancelledAt(now);

        AdminCoupon savedCoupon = couponRepository.save(coupon);

        // Send notification if requested
        if (Boolean.TRUE.equals(request.getSendNotification()) && coupon.getBeneficiaryId() != null) {
            eventProducer.sendCouponCancelledEvent(savedCoupon);
        }

        log.info("Coupon {} cancelled successfully", coupon.getCouponCode());

        return couponMapper.toDto(savedCoupon);
    }

    /**
     * Cancel all coupons in a batch
     */
    public CouponBatchDto cancelBatch(Long batchId, CancelCouponRequest request, Long adminUserId) {
        log.info("Admin {} cancelling batch {}", adminUserId, batchId);

        AdminCouponBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("Batch not found", HttpStatus.NOT_FOUND));

        // Cancel all non-used coupons in batch
        LocalDateTime now = LocalDateTime.now();
        int cancelledCount = couponRepository.cancelBatch(batchId, request.getReason(), adminUserId, now);

        // Update batch status
        batch.setStatus(CouponBatchStatus.CANCELLED);
        AdminCouponBatch savedBatch = batchRepository.save(batch);

        log.info("Cancelled {} coupons from batch {}", cancelledCount, batch.getBatchCode());

        // Send notification
        if (Boolean.TRUE.equals(request.getSendNotification())) {
            eventProducer.sendBatchCancelledEvent(savedBatch, cancelledCount, request.getReason());
        }

        return couponMapper.toBatchDto(savedBatch);
    }

    // ==================== Coupon Retrieval ====================

    /**
     * Get coupon by ID
     */
    @Transactional(readOnly = true)
    public AdminCouponDto getCouponById(Long couponId) {
        AdminCoupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException("Coupon not found", HttpStatus.NOT_FOUND));
        return couponMapper.toDto(coupon);
    }

    /**
     * Get coupon by code
     */
    @Transactional(readOnly = true)
    public AdminCouponDto getCouponByCode(String couponCode) {
        couponCode = codeGenerator.normalizeCouponCode(couponCode);
        AdminCoupon coupon = couponRepository.findByCouponCodeAndIsDeletedFalse(couponCode)
                .orElseThrow(() -> new BusinessException("Coupon not found", HttpStatus.NOT_FOUND));
        return couponMapper.toDto(coupon);
    }

    /**
     * Get all coupons with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<AdminCouponDto> getCoupons(AdminCouponStatus status, BeneficiaryType beneficiaryType,
                                           Long beneficiaryId, Long batchId, String couponCode,
                                           Pageable pageable) {
        Page<AdminCoupon> coupons = couponRepository.searchCoupons(
                status, beneficiaryType, beneficiaryId, batchId, couponCode, pageable);
        return coupons.map(couponMapper::toDto);
    }

    /**
     * Get coupons for a specific beneficiary
     */
    @Transactional(readOnly = true)
    public List<AdminCouponDto> getCouponsForBeneficiary(BeneficiaryType type, Long beneficiaryId) {
        List<AdminCoupon> coupons = couponRepository
                .findByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(type, beneficiaryId);
        return couponMapper.toDtoList(coupons);
    }

    /**
     * Get available coupons for a beneficiary
     */
    @Transactional(readOnly = true)
    public List<AdminCouponDto> getAvailableCouponsForBeneficiary(BeneficiaryType type, Long beneficiaryId) {
        List<AdminCoupon> coupons = couponRepository
                .findAvailableCouponsForBeneficiary(type, beneficiaryId, LocalDateTime.now());
        return couponMapper.toDtoList(coupons);
    }

    /**
     * Get expiring coupons
     */
    @Transactional(readOnly = true)
    public List<AdminCouponDto> getExpiringCoupons(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(days);
        List<AdminCoupon> coupons = couponRepository.findCouponsExpiringSoon(now, futureDate);
        return couponMapper.toDtoList(coupons);
    }

    // ==================== Batch Retrieval ====================

    /**
     * Get batch by ID
     */
    @Transactional(readOnly = true)
    public CouponBatchDto getBatchById(Long batchId) {
        AdminCouponBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("Batch not found", HttpStatus.NOT_FOUND));
        return enrichBatchWithCounts(couponMapper.toBatchDto(batch), batchId);
    }

    /**
     * Get all batches with pagination
     */
    @Transactional(readOnly = true)
    public Page<CouponBatchDto> getBatches(CouponBatchStatus status, BeneficiaryType beneficiaryType,
                                           Long beneficiaryId, String batchCode, Pageable pageable) {
        Page<AdminCouponBatch> batches = batchRepository.searchBatches(
                status, beneficiaryType, beneficiaryId, batchCode, pageable);
        return batches.map(batch -> {
            CouponBatchDto dto = couponMapper.toBatchDto(batch);
            return enrichBatchWithCounts(dto, batch.getId());
        });
    }

    private CouponBatchDto enrichBatchWithCounts(CouponBatchDto dto, Long batchId) {
        dto.setAvailableCoupons((int) couponRepository.countByBatchIdAndStatus(batchId, AdminCouponStatus.DISTRIBUTED));
        dto.setUsedCoupons((int) couponRepository.countByBatchIdAndStatus(batchId, AdminCouponStatus.USED));
        dto.setExpiredCoupons((int) couponRepository.countByBatchIdAndStatus(batchId, AdminCouponStatus.EXPIRED));
        dto.setCancelledCoupons((int) couponRepository.countByBatchIdAndStatus(batchId, AdminCouponStatus.CANCELLED));
        return dto;
    }

    // ==================== Statistics & Analytics ====================

    /**
     * Get coupon summary statistics
     */
    @Transactional(readOnly = true)
    public CouponSummaryDto getCouponSummary() {
        return CouponSummaryDto.builder()
                .totalCoupons((int) couponRepository.count())
                .createdCoupons((int) couponRepository.countByStatusAndIsDeletedFalse(AdminCouponStatus.CREATED))
                .distributedCoupons((int) couponRepository.countByStatusAndIsDeletedFalse(AdminCouponStatus.DISTRIBUTED))
                .usedCoupons((int) couponRepository.countByStatusAndIsDeletedFalse(AdminCouponStatus.USED))
                .expiredCoupons((int) couponRepository.countByStatusAndIsDeletedFalse(AdminCouponStatus.EXPIRED))
                .cancelledCoupons((int) couponRepository.countByStatusAndIsDeletedFalse(AdminCouponStatus.CANCELLED))
                .expiringSoonCoupons(getExpiringCoupons(30).size())
                .totalRedeemedValue(redemptionRepository.sumTotalDiscountsApplied())
                .build();
    }

    /**
     * Get coupon summary for a specific beneficiary
     */
    @Transactional(readOnly = true)
    public CouponSummaryDto getCouponSummaryForBeneficiary(BeneficiaryType type, Long beneficiaryId) {
        LocalDateTime now = LocalDateTime.now();
        
        long available = couponRepository.countAvailableCouponsForBeneficiary(type, beneficiaryId, now);
        BigDecimal availableValue = couponRepository.sumAvailableValueForBeneficiary(type, beneficiaryId, now);
        
        List<AdminCoupon> allCoupons = couponRepository
                .findByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(type, beneficiaryId);
        
        long used = allCoupons.stream().filter(c -> c.getStatus() == AdminCouponStatus.USED).count();
        long expired = allCoupons.stream().filter(c -> c.getStatus() == AdminCouponStatus.EXPIRED).count();
        long expiringSoon = allCoupons.stream().filter(AdminCoupon::isExpiringSoon).count();

        return CouponSummaryDto.builder()
                .totalCoupons(allCoupons.size())
                .distributedCoupons((int) available)
                .usedCoupons((int) used)
                .expiredCoupons((int) expired)
                .expiringSoonCoupons((int) expiringSoon)
                .totalAvailableValue(availableValue)
                .build();
    }

    // ==================== Scheduled Tasks ====================

    /**
     * Scheduled task to expire coupons
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void expireCoupons() {
        log.info("Running scheduled coupon expiration task");

        LocalDateTime now = LocalDateTime.now();
        List<AdminCoupon> expiredCoupons = couponRepository.findExpiredCoupons(now);

        if (expiredCoupons.isEmpty()) {
            log.info("No coupons to expire");
            return;
        }

        // Update status
        int updated = couponRepository.updateExpiredCoupons(now);
        log.info("Expired {} coupons", updated);

        // Send expiration events
        eventProducer.sendCouponsExpiredEvent(expiredCoupons);

        // Update batch statuses
        expiredCoupons.stream()
                .filter(c -> c.getBatchId() != null)
                .map(AdminCoupon::getBatchId)
                .distinct()
                .forEach(this::updateBatchStatusAfterExpiration);
    }

    private void updateBatchStatusAfterExpiration(Long batchId) {
        List<AdminCoupon> batchCoupons = couponRepository.findByBatchIdAndIsDeletedFalse(batchId);
        boolean allExpiredOrUsed = batchCoupons.stream()
                .allMatch(c -> c.getStatus() == AdminCouponStatus.EXPIRED || 
                              c.getStatus() == AdminCouponStatus.USED);

        if (allExpiredOrUsed) {
            boolean anyUsed = batchCoupons.stream()
                    .anyMatch(c -> c.getStatus() == AdminCouponStatus.USED);
            CouponBatchStatus newStatus = anyUsed ? CouponBatchStatus.FULLY_USED : CouponBatchStatus.EXPIRED;
            batchRepository.updateBatchStatus(batchId, newStatus, LocalDateTime.now());
        }
    }

    /**
     * Scheduled task to send expiring soon notifications
     * Runs daily at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpiringCouponNotifications() {
        log.info("Running scheduled expiring coupon notification task");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysLater = now.plusDays(30);
        
        List<AdminCoupon> expiringCoupons = couponRepository.findCouponsExpiringSoon(now, thirtyDaysLater);

        if (expiringCoupons.isEmpty()) {
            log.info("No coupons expiring soon");
            return;
        }

        // Group by beneficiary and send notifications
        expiringCoupons.stream()
                .collect(Collectors.groupingBy(c -> c.getBeneficiaryType() + "_" + c.getBeneficiaryId()))
                .forEach((key, coupons) -> {
                    if (!coupons.isEmpty()) {
                        eventProducer.sendCouponsExpiringSoonNotification(coupons);
                    }
                });

        log.info("Sent expiring notifications for {} coupons", expiringCoupons.size());
    }

    // ==================== Helper Methods ====================

    private String generateUniqueCouponCode() {
        String code;
        int attempts = 0;
        do {
            code = codeGenerator.generateCouponCode();
            attempts++;
            if (attempts > 10) {
                code = codeGenerator.generateUUIDCode();
                break;
            }
        } while (couponRepository.existsByCouponCode(code));
        return code;
    }
}