package com.adminservice.kafka;

import com.adminservice.entity.AdminCoupon;
import com.adminservice.entity.AdminCouponBatch;
import com.adminservice.mapper.AdminCouponMapper;
import com.commonlibrary.constants.CouponKafkaTopics;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kafka event producer for coupon-related events.
 * Publishes events for coupon creation, distribution, usage, cancellation, and expiration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdminCouponMapper couponMapper;

    // ==================== Coupon Created Events ====================

    /**
     * Send event when a coupon is created
     */
    public void sendCouponCreatedEvent(AdminCoupon coupon) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_CREATED");
            event.put("couponId", coupon.getId());
            event.put("couponCode", coupon.getCouponCode());
            event.put("discountType", coupon.getDiscountType().name());
            event.put("discountValue", coupon.getDiscountValue());
            event.put("beneficiaryType", coupon.getBeneficiaryType().name());
            event.put("createdBy", coupon.getCreatedBy());
            event.put("expiresAt", coupon.getExpiresAt().toString());
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.COUPON_CREATED, event);
            log.info("Coupon created event sent: {}", coupon.getCouponCode());

        } catch (Exception e) {
            log.error("Error sending coupon created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send event when a batch is created
     */
    public void sendBatchCreatedEvent(AdminCouponBatch batch, int couponCount) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BATCH_CREATED");
            event.put("batchId", batch.getId());
            event.put("batchCode", batch.getBatchCode());
            event.put("totalCoupons", couponCount);
            event.put("discountType", batch.getDiscountType().name());
            event.put("discountValue", batch.getDiscountValue());
            event.put("beneficiaryType", batch.getBeneficiaryType().name());
            event.put("createdBy", batch.getCreatedBy());
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.COUPON_BATCH_CREATED, event);
            log.info("Batch created event sent: {} with {} coupons", batch.getBatchCode(), couponCount);

        } catch (Exception e) {
            log.error("Error sending batch created event: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Distributed Events ====================

    /**
     * Send event when a coupon is distributed to a beneficiary
     * This event is consumed by supervisor-service or patient-service
     */
    public void sendCouponDistributedEvent(AdminCoupon coupon) {
        try {
            CouponDistributionEvent event = couponMapper.toDistributionEvent(coupon);
            
            kafkaTemplate.send(CouponKafkaTopics.COUPON_DISTRIBUTED, event);
            log.info("Coupon distributed event sent: {} to {} {}", 
                    coupon.getCouponCode(), coupon.getBeneficiaryType(), coupon.getBeneficiaryId());

        } catch (Exception e) {
            log.error("Error sending coupon distributed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to beneficiary about coupon distribution
     */
    public void sendCouponDistributedNotification(AdminCoupon coupon) {
        try {
            String title = "New Coupon Received";
            String message = String.format(
                    "You have received a new coupon!\n\n" +
                    "Coupon Code: %s\n" +
                    "Discount: %s %s\n" +
                    "Expires: %s\n\n" +
                    "Use this coupon for your next consultation payment.",
                    coupon.getCouponCode(),
                    coupon.getDiscountType().name().equals("PERCENTAGE") 
                            ? coupon.getDiscountValue() + "%" 
                            : "$" + coupon.getDiscountValue(),
                    coupon.getDiscountType().name().equals("FULL_COVERAGE") ? "(Full Coverage)" : "",
                    coupon.getExpiresAt().toLocalDate()
            );

            NotificationDto notification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(coupon.getBeneficiaryId())
                    .title(title)
                    .message(message)
                    .type(NotificationType.COUPON_ISSUED)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", notification);
            log.info("Coupon distribution notification sent to beneficiary: {}", coupon.getBeneficiaryId());

        } catch (Exception e) {
            log.error("Error sending coupon distribution notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification for batch distribution
     */
    public void sendBatchDistributedNotification(AdminCouponBatch batch, int couponCount) {
        try {
            String title = "Coupons Allocated";
            String message = String.format(
                    "You have received %d new coupons!\n\n" +
                    "Batch: %s\n" +
                    "Discount per coupon: %s\n" +
                    "Valid for: %d days\n\n" +
                    "These coupons can be assigned to your patients for consultation payments.",
                    couponCount,
                    batch.getBatchCode(),
                    batch.getDiscountType().name().equals("PERCENTAGE")
                            ? batch.getDiscountValue() + "%"
                            : "$" + batch.getDiscountValue(),
                    batch.getExpiryDays()
            );

            NotificationDto notification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(batch.getBeneficiaryId())
                    .title(title)
                    .message(message)
                    .type(NotificationType.COUPON_ISSUED)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", notification);
            
            // Also send batch distributed event
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BATCH_DISTRIBUTED");
            event.put("batchId", batch.getId());
            event.put("batchCode", batch.getBatchCode());
            event.put("beneficiaryType", batch.getBeneficiaryType().name());
            event.put("beneficiaryId", batch.getBeneficiaryId());
            event.put("couponCount", couponCount);
            event.put("distributedBy", batch.getDistributedBy());
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.COUPON_BATCH_DISTRIBUTED, event);
            
            log.info("Batch distribution notification sent: {} coupons to {}", 
                    couponCount, batch.getBeneficiaryId());

        } catch (Exception e) {
            log.error("Error sending batch distribution notification: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Used Events ====================

    /**
     * Send event when a coupon is used (redeemed)
     * This event is consumed by supervisor-service and payment-service
     */
    public void sendCouponUsedEvent(AdminCoupon coupon, MarkCouponUsedRequest request) {
        try {
            CouponUsedEvent event = couponMapper.toUsedEvent(coupon, request);
            
            kafkaTemplate.send(CouponKafkaTopics.COUPON_USED, event);
            log.info("Coupon used event sent: {} for case {}", 
                    coupon.getCouponCode(), request.getCaseId());

        } catch (Exception e) {
            log.error("Error sending coupon used event: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Cancelled Events ====================

    /**
     * Send event when a coupon is cancelled
     * This event is consumed by supervisor-service and patient-service
     */
    public void sendCouponCancelledEvent(AdminCoupon coupon) {
        try {
            CouponCancelledEvent event = couponMapper.toCancelledEvent(coupon);
            
            kafkaTemplate.send(CouponKafkaTopics.COUPON_CANCELLED, event);
            
            // Also send notification
            String title = "Coupon Cancelled";
            String message = String.format(
                    "Your coupon has been cancelled.\n\n" +
                    "Coupon Code: %s\n" +
                    "Reason: %s\n\n" +
                    "If you believe this is an error, please contact support.",
                    coupon.getCouponCode(),
                    coupon.getCancellationReason() != null ? coupon.getCancellationReason() : "Not specified"
            );

            NotificationDto notification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(coupon.getBeneficiaryId())
                    .title(title)
                    .message(message)
                    .type(NotificationType.COUPON_CANCELLED)
                    .sendEmail(true)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", notification);
            
            log.info("Coupon cancelled event sent: {}", coupon.getCouponCode());

        } catch (Exception e) {
            log.error("Error sending coupon cancelled event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send event when a batch is cancelled
     */
    public void sendBatchCancelledEvent(AdminCouponBatch batch, int cancelledCount, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BATCH_CANCELLED");
            event.put("batchId", batch.getId());
            event.put("batchCode", batch.getBatchCode());
            event.put("beneficiaryType", batch.getBeneficiaryType().name());
            event.put("beneficiaryId", batch.getBeneficiaryId());
            event.put("cancelledCount", cancelledCount);
            event.put("reason", reason);
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.COUPON_CANCELLED, event);
            
            // Send notification if beneficiary was assigned
            if (batch.getBeneficiaryId() != null) {
                String title = "Coupons Cancelled";
                String message = String.format(
                        "%d coupons from batch %s have been cancelled.\n\n" +
                        "Reason: %s\n\n" +
                        "If you believe this is an error, please contact support.",
                        cancelledCount,
                        batch.getBatchCode(),
                        reason != null ? reason : "Not specified"
                );

                NotificationDto notification = NotificationDto.builder()
                        .senderId(0L)
                        .receiverId(batch.getBeneficiaryId())
                        .title(title)
                        .message(message)
                        .type(NotificationType.COUPON_CANCELLED)
                        .sendEmail(true)
                        .priority(NotificationPriority.HIGH)
                        .build();

                kafkaTemplate.send("notification-topic", notification);
            }
            
            log.info("Batch cancelled event sent: {} ({} coupons)", batch.getBatchCode(), cancelledCount);

        } catch (Exception e) {
            log.error("Error sending batch cancelled event: {}", e.getMessage(), e);
        }
    }

    // ==================== Coupon Expired Events ====================

    /**
     * Send events for expired coupons (called by scheduled task)
     */
    public void sendCouponsExpiredEvent(List<AdminCoupon> expiredCoupons) {
        try {
            // Group by beneficiary and send events
            Map<String, List<AdminCoupon>> groupedCoupons = expiredCoupons.stream()
                    .filter(c -> c.getBeneficiaryId() != null)
                    .collect(Collectors.groupingBy(c -> c.getBeneficiaryType() + "_" + c.getBeneficiaryId()));

            groupedCoupons.forEach((key, coupons) -> {
                String[] parts = key.split("_");
                BeneficiaryType type = BeneficiaryType.valueOf(parts[0]);
                Long beneficiaryId = Long.parseLong(parts[1]);

                CouponExpiredEvent event = CouponExpiredEvent.builder()
                        .eventType("COUPONS_EXPIRED")
                        .couponIds(coupons.stream().map(AdminCoupon::getId).collect(Collectors.toList()))
                        .couponCodes(coupons.stream().map(AdminCoupon::getCouponCode).collect(Collectors.toList()))
                        .beneficiaryType(type)
                        .beneficiaryId(beneficiaryId)
                        .expiredAt(LocalDateTime.now())
                        .timestamp(LocalDateTime.now())
                        .build();

                kafkaTemplate.send(CouponKafkaTopics.COUPON_EXPIRED, event);
            });

            log.info("Expired events sent for {} coupons", expiredCoupons.size());

        } catch (Exception e) {
            log.error("Error sending coupons expired events: {}", e.getMessage(), e);
        }
    }

    // ==================== Expiring Soon Notifications ====================

    /**
     * Send notification for coupons expiring soon
     */
    public void sendCouponsExpiringSoonNotification(List<AdminCoupon> expiringCoupons) {
        if (expiringCoupons.isEmpty()) return;

        try {
            AdminCoupon firstCoupon = expiringCoupons.get(0);
            
            String title = "Coupons Expiring Soon";
            String message;
            
            if (expiringCoupons.size() == 1) {
                message = String.format(
                        "Your coupon %s will expire on %s.\n\n" +
                        "Use it before it expires!",
                        firstCoupon.getCouponCode(),
                        firstCoupon.getExpiresAt().toLocalDate()
                );
            } else {
                message = String.format(
                        "You have %d coupons expiring soon:\n\n%s\n\n" +
                        "Use them before they expire!",
                        expiringCoupons.size(),
                        expiringCoupons.stream()
                                .limit(5)
                                .map(c -> String.format("• %s (expires %s)", 
                                        c.getCouponCode(), c.getExpiresAt().toLocalDate()))
                                .collect(Collectors.joining("\n"))
                        + (expiringCoupons.size() > 5 ? "\n...and " + (expiringCoupons.size() - 5) + " more" : "")
                );
            }

            NotificationDto notification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(firstCoupon.getBeneficiaryId())
                    .title(title)
                    .message(message)
                    .type(NotificationType.COUPON_EXPIRING_SOON)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", notification);
            
            // Also send event for service consumption
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPONS_EXPIRING_SOON");
            event.put("beneficiaryType", firstCoupon.getBeneficiaryType().name());
            event.put("beneficiaryId", firstCoupon.getBeneficiaryId());
            event.put("couponCount", expiringCoupons.size());
            event.put("couponCodes", expiringCoupons.stream()
                    .map(AdminCoupon::getCouponCode)
                    .collect(Collectors.toList()));
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.COUPON_EXPIRING_SOON, event);
            
            log.info("Expiring soon notification sent: {} coupons for beneficiary {}", 
                    expiringCoupons.size(), firstCoupon.getBeneficiaryId());

        } catch (Exception e) {
            log.error("Error sending expiring soon notification: {}", e.getMessage(), e);
        }
    }
}