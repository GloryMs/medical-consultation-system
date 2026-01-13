package com.paymentservice.kafka;

import com.commonlibrary.constants.CouponKafkaTopics;
import com.commonlibrary.dto.coupon.CouponUsedEvent;
import com.commonlibrary.entity.PaymentStatus;
import com.paymentservice.entity.Payment;
import com.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka consumer for coupon events from admin-service.
 * Updates payment records based on coupon lifecycle events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminCouponEventConsumer {

    private final PaymentRepository paymentRepository;

    /**
     * Handle coupon used event from admin-service.
     * Confirms that coupon was successfully marked as used.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_USED,
            groupId = "payment-service-coupon-group"
    )
    @Transactional
    public void handleCouponUsed(Map<String, Object> event) {
        try {
            log.info("Received coupon used event: {}", event);

            Long paymentId = event.get("paymentId") != null 
                    ? Long.valueOf(event.get("paymentId").toString()) 
                    : null;
            String couponCode = event.get("couponCode") != null 
                    ? event.get("couponCode").toString() 
                    : null;
            Long caseId = event.get("caseId") != null 
                    ? Long.valueOf(event.get("caseId").toString()) 
                    : null;

            if (paymentId != null) {
                // Update payment with coupon usage confirmation
                paymentRepository.findById(paymentId).ifPresent(payment -> {
                    payment.addMetadata("coupon_confirmed_used", "true");
                    payment.addMetadata("coupon_used_event_received", LocalDateTime.now().toString());
                    paymentRepository.save(payment);
                    log.info("Updated payment {} with coupon usage confirmation", paymentId);
                });
            } else if (caseId != null && couponCode != null) {
                // Find by case and coupon code
                log.info("Coupon {} used for case {} confirmed", couponCode, caseId);
            }

        } catch (Exception e) {
            log.error("Error handling coupon used event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle coupon cancelled event from admin-service.
     * May need to handle refunds if payment was in progress.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_CANCELLED,
            groupId = "payment-service-coupon-group"
    )
    public void handleCouponCancelled(Map<String, Object> event) {
        try {
            log.info("Received coupon cancelled event: {}", event);

            String couponCode = event.get("couponCode") != null 
                    ? event.get("couponCode").toString() 
                    : null;
            Long couponId = event.get("couponId") != null 
                    ? Long.valueOf(event.get("couponId").toString()) 
                    : null;

            // Check if any pending payments were using this coupon
            // For now, just log the event
            log.info("Coupon {} (ID: {}) was cancelled", couponCode, couponId);

        } catch (Exception e) {
            log.error("Error handling coupon cancelled event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle coupon expired event from admin-service.
     * May need to handle pending payments with expired coupons.
     */
    @KafkaListener(
            topics = CouponKafkaTopics.COUPON_EXPIRED,
            groupId = "payment-service-coupon-group"
    )
    public void handleCouponExpired(Map<String, Object> event) {
        try {
            log.info("Received coupon expired event: {}", event);

            // Log for audit purposes
            String eventType = event.get("eventType") != null 
                    ? event.get("eventType").toString() 
                    : "UNKNOWN";

            log.info("Coupon expiration event received: {}", eventType);

        } catch (Exception e) {
            log.error("Error handling coupon expired event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle supervisor coupon redeemed event.
     * This is for tracking when supervisors use coupons for their patients.
     */
    @KafkaListener(
            topics = "supervisor.coupon.redeemed",
            groupId = "payment-service-coupon-group"
    )
    @Transactional
    public void handleSupervisorCouponRedeemed(Map<String, Object> event) {
        try {
            log.info("Received supervisor coupon redeemed event: {}", event);

            Long supervisorId = event.get("supervisorId") != null 
                    ? Long.valueOf(event.get("supervisorId").toString()) 
                    : null;
            Long patientId = event.get("patientId") != null 
                    ? Long.valueOf(event.get("patientId").toString()) 
                    : null;
            Long caseId = event.get("caseId") != null 
                    ? Long.valueOf(event.get("caseId").toString()) 
                    : null;
            String couponCode = event.get("couponCode") != null 
                    ? event.get("couponCode").toString() 
                    : null;
            BigDecimal discountAmount = event.get("discountAmount") != null 
                    ? new BigDecimal(event.get("discountAmount").toString()) 
                    : null;

            log.info("Supervisor {} redeemed coupon {} for patient {} on case {} - discount: {}",
                    supervisorId, couponCode, patientId, caseId, discountAmount);

            // Record coupon usage in payment history (for reconciliation)
            // This could create an audit entry or update payment metadata

        } catch (Exception e) {
            log.error("Error handling supervisor coupon redeemed event: {}", e.getMessage(), e);
        }
    }
}