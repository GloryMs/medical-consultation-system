package com.supervisorservice.kafka;

import com.commonlibrary.constants.CouponKafkaTopics;
import com.commonlibrary.dto.coupon.CouponCancelledEvent;
import com.commonlibrary.dto.coupon.CouponDistributionEvent;
import com.commonlibrary.dto.coupon.CouponExpiredEvent;
import com.commonlibrary.dto.coupon.CouponUsedEvent;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.SupervisorCouponStatus;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import com.supervisorservice.repository.SupervisorCouponAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import com.commonlibrary.entity.UserType;

/**
 * Kafka consumer for coupon events from admin-service.
 * Updates local coupon allocations based on admin-service events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminCouponEventConsumer {

    private final SupervisorCouponAllocationRepository allocationRepository;

    // ==================== Coupon Distributed Event ====================

    /**
     * Handle coupon distributed event from admin-service.
     * Creates a local allocation record for the supervisor.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_DISTRIBUTED,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP,
            containerFactory = "couponKafkaListenerContainerFactory")

    @Transactional
    public void handleCouponDistributed(CouponDistributionEvent event) {
        try {
            log.info("Received coupon distributed event: {} for {} {}",
                    event.getCouponCode(), event.getBeneficiaryType(), event.getBeneficiaryId());

            // Only process events for supervisors
            if (event.getBeneficiaryType() != BeneficiaryType.MEDICAL_SUPERVISOR) {
                log.debug("Ignoring coupon distribution for non-supervisor beneficiary: {}",
                        event.getBeneficiaryType());
                return;
            }

            // Check if allocation already exists
            if (allocationRepository.existsByAdminCouponId(event.getCouponId())) {
                log.warn("Coupon allocation already exists for admin coupon ID: {}", event.getCouponId());
                return;
            }

            // Create local allocation
            SupervisorCouponAllocation allocation = SupervisorCouponAllocation.builder()
                    .adminCouponId(event.getCouponId())
                    .couponCode(event.getCouponCode())
                    .supervisorId(event.getBeneficiaryId())
                    .discountType(event.getDiscountType())
                    .discountValue(event.getDiscountValue())
                    .maxDiscountAmount(event.getMaxDiscountAmount())
                    .currency(event.getCurrency() != null ? event.getCurrency() : "USD")
                    .status(SupervisorCouponStatus.AVAILABLE)
                    .expiresAt(event.getExpiresAt())
                    .receivedAt(LocalDateTime.now())
                    .lastSyncedAt(LocalDateTime.now())
                    .batchId(event.getBatchId())
                    .batchCode(event.getBatchCode())
                    .build();

            allocationRepository.save(allocation);
            log.info("Created local coupon allocation: {} for supervisor {}",
                    event.getCouponCode(), event.getBeneficiaryId());

        } catch (Exception e) {
            log.error("Error handling coupon distributed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Alternative handler for Map-based events (backward compatibility)
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_DISTRIBUTED,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP + "-map",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCouponDistributedMap(Map<String, Object> event) {
        try {
            String beneficiaryType = event.get("beneficiaryType").toString();
            
            // Only process events for supervisors
            if (!"MEDICAL_SUPERVISOR".equals(beneficiaryType)) {
                return;
            }

            Long couponId = Long.valueOf(event.get("couponId").toString());
            
            // Check if already processed
            if (allocationRepository.existsByAdminCouponId(couponId)) {
                return;
            }

            String couponCode = event.get("couponCode").toString();
            Long beneficiaryId = Long.valueOf(event.get("beneficiaryId").toString());
            String discountType = event.get("discountType").toString();
            java.math.BigDecimal discountValue = new java.math.BigDecimal(event.get("discountValue").toString());
            
            java.math.BigDecimal maxDiscountAmount = null;
            if (event.get("maxDiscountAmount") != null) {
                maxDiscountAmount = new java.math.BigDecimal(event.get("maxDiscountAmount").toString());
            }
            
            String currency = event.get("currency") != null ? event.get("currency").toString() : "USD";
            LocalDateTime expiresAt = LocalDateTime.parse(event.get("expiresAt").toString());

            Long batchId = event.get("batchId") != null ? Long.valueOf(event.get("batchId").toString()) : null;
            String batchCode = event.get("batchCode") != null ? event.get("batchCode").toString() : null;

            SupervisorCouponAllocation allocation = SupervisorCouponAllocation.builder()
                    .adminCouponId(couponId)
                    .couponCode(couponCode)
                    .supervisorId(beneficiaryId)
                    .discountType(com.commonlibrary.entity.DiscountType.valueOf(discountType))
                    .discountValue(discountValue)
                    .maxDiscountAmount(maxDiscountAmount)
                    .currency(currency)
                    .status(SupervisorCouponStatus.AVAILABLE)
                    .expiresAt(expiresAt)
                    .receivedAt(LocalDateTime.now())
                    .lastSyncedAt(LocalDateTime.now())
                    .batchId(batchId)
                    .batchCode(batchCode)
                    .build();

            allocationRepository.save(allocation);
            log.info("Created local coupon allocation (map): {} for supervisor {}", couponCode, beneficiaryId);

        } catch (Exception e) {
            log.error("Error handling coupon distributed map event: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Used Event ====================

    /**
     * Handle coupon used event from admin-service.
     * Updates local allocation status to USED.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_USED,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP,
            containerFactory = "couponKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCouponUsed(CouponUsedEvent event) {
        try {
            log.info("Received coupon used event: {} for case {}",
                    event.getCouponCode(), event.getCaseId());

            // Only process events for supervisors
            if (event.getBeneficiaryType() != BeneficiaryType.MEDICAL_SUPERVISOR) {
                return;
            }

            int updated = allocationRepository.markAsUsed(
                    event.getCouponId(),
                    event.getCaseId(),
                    event.getPaymentId(),
                    event.getUsedAt() != null ? event.getUsedAt() : LocalDateTime.now()
            );

            if (updated > 0) {
                log.info("Updated local allocation to USED: {}", event.getCouponCode());
            } else {
                log.warn("No local allocation found to update for coupon: {}", event.getCouponCode());
            }

        } catch (Exception e) {
            log.error("Error handling coupon used event: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Cancelled Event ====================

    /**
     * Handle coupon cancelled event from admin-service.
     * Updates local allocation status to CANCELLED.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_CANCELLED,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP,
            containerFactory = "couponKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCouponCancelled(CouponCancelledEvent event) {
        try {
            log.info("Received coupon cancelled event: {}", event.getCouponCode());

            // Only process events for supervisors
            if (event.getBeneficiaryType() != BeneficiaryType.MEDICAL_SUPERVISOR) {
                return;
            }

            int updated = allocationRepository.markAsCancelled(
                    event.getCouponId(),
                    event.getCancelledAt() != null ? event.getCancelledAt() : LocalDateTime.now()
            );

            if (updated > 0) {
                log.info("Updated local allocation to CANCELLED: {}", event.getCouponCode());
            } else {
                log.warn("No local allocation found to cancel for coupon: {}", event.getCouponCode());
            }

        } catch (Exception e) {
            log.error("Error handling coupon cancelled event: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Expired Event ====================

    /**
     * Handle coupon expired event from admin-service.
     * Updates local allocation status to EXPIRED.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_EXPIRED,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP,
            containerFactory = "couponKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCouponExpired(CouponExpiredEvent event) {
        try {
            log.info("Received coupon expired event for {} coupons",
                    event.getCouponIds() != null ? event.getCouponIds().size() : 0);

            // Only process events for supervisors
            if (event.getBeneficiaryType() != BeneficiaryType.MEDICAL_SUPERVISOR) {
                return;
            }

            if (event.getCouponIds() == null || event.getCouponIds().isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int totalUpdated = 0;

            for (Long couponId : event.getCouponIds()) {
                int updated = allocationRepository.updateStatusByAdminCouponId(
                        couponId,
                        SupervisorCouponStatus.EXPIRED,
                        now
                );
                totalUpdated += updated;
            }

            log.info("Updated {} local allocations to EXPIRED", totalUpdated);

        } catch (Exception e) {
            log.error("Error handling coupon expired event: {}", e.getMessage(), e);
        }
    }

    // ==================== Batch Distributed Event ====================

    /**
     * Handle batch distributed event from admin-service.
     * This is handled by individual coupon distribution events, but we log batch info.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_BATCH_DISTRIBUTED,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP
    )
    public void handleBatchDistributed(Map<String, Object> event) {
        try {
            String beneficiaryType = event.get("beneficiaryType").toString();
            
            // Only process events for supervisors
            if (!"MEDICAL_SUPERVISOR".equals(beneficiaryType)) {
                return;
            }

            String batchCode = event.get("batchCode").toString();
            Long beneficiaryId = Long.valueOf(event.get("beneficiaryId").toString());
            int couponCount = Integer.parseInt(event.get("couponCount").toString());

            log.info("Batch {} distributed to supervisor {} with {} coupons",
                    batchCode, beneficiaryId, couponCount);

            // Individual coupons will be processed via COUPON_DISTRIBUTED events

        } catch (Exception e) {
            log.error("Error handling batch distributed event: {}", e.getMessage(), e);
        }
    }

    // ==================== Expiring Soon Notification ====================

    /**
     * Handle expiring soon notification from admin-service.
     * Could trigger local notifications or dashboard updates.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_EXPIRING_SOON,
            groupId = CouponKafkaTopics.SUPERVISOR_SERVICE_GROUP
    )
    public void handleCouponsExpiringSoon(Map<String, Object> event) {
        try {
            String beneficiaryType = event.get("beneficiaryType").toString();
            
            // Only process events for supervisors
            if (!"MEDICAL_SUPERVISOR".equals(beneficiaryType)) {
                return;
            }

            Long beneficiaryId = Long.valueOf(event.get("beneficiaryId").toString());
            int couponCount = Integer.parseInt(event.get("couponCount").toString());

            log.info("Supervisor {} has {} coupons expiring soon", beneficiaryId, couponCount);

            // This could trigger local notifications or dashboard updates
            // For now, just log the event

        } catch (Exception e) {
            log.error("Error handling expiring soon event: {}", e.getMessage(), e);
        }
    }
}