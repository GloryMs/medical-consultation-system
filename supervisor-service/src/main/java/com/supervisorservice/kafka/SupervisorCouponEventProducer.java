package com.supervisorservice.kafka;

import com.commonlibrary.constants.CouponKafkaTopics;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer for supervisor coupon events.
 * Publishes events when supervisor assigns/unassigns coupons to patients.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorCouponEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send event when coupon is assigned to a patient
     */
    public void sendCouponAssignedEvent(SupervisorCouponAllocation allocation, Long supervisorId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SUPERVISOR_COUPON_ASSIGNED");
            event.put("allocationId", allocation.getId());
            event.put("adminCouponId", allocation.getAdminCouponId());
            event.put("couponCode", allocation.getCouponCode());
            event.put("supervisorId", supervisorId);
            event.put("patientId", allocation.getAssignedPatientId());
            event.put("assignedAt", allocation.getAssignedAt().toString());
            event.put("discountType", allocation.getDiscountType().name());
            event.put("discountValue", allocation.getDiscountValue());
            event.put("expiresAt", allocation.getExpiresAt().toString());
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.SUPERVISOR_COUPON_ASSIGNED, event);
            log.info("Coupon assigned event sent: {} to patient {}", 
                    allocation.getCouponCode(), allocation.getAssignedPatientId());

        } catch (Exception e) {
            log.error("Error sending coupon assigned event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send event when coupon is unassigned from a patient
     */
    public void sendCouponUnassignedEvent(
            SupervisorCouponAllocation allocation, 
            Long supervisorId, 
            Long previousPatientId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SUPERVISOR_COUPON_UNASSIGNED");
            event.put("allocationId", allocation.getId());
            event.put("adminCouponId", allocation.getAdminCouponId());
            event.put("couponCode", allocation.getCouponCode());
            event.put("supervisorId", supervisorId);
            event.put("previousPatientId", previousPatientId);
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(CouponKafkaTopics.SUPERVISOR_COUPON_UNASSIGNED, event);
            log.info("Coupon unassigned event sent: {} from patient {}", 
                    allocation.getCouponCode(), previousPatientId);

        } catch (Exception e) {
            log.error("Error sending coupon unassigned event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send event when coupon is about to be used for payment
     */
    public void sendCouponRedemptionInitiatedEvent(
            SupervisorCouponAllocation allocation,
            Long supervisorId,
            Long caseId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_REDEMPTION_INITIATED");
            event.put("allocationId", allocation.getId());
            event.put("adminCouponId", allocation.getAdminCouponId());
            event.put("couponCode", allocation.getCouponCode());
            event.put("supervisorId", supervisorId);
            event.put("patientId", allocation.getAssignedPatientId());
            event.put("caseId", caseId);
            event.put("discountType", allocation.getDiscountType().name());
            event.put("discountValue", allocation.getDiscountValue());
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("supervisor.coupon.redemption-initiated", event);
            log.info("Coupon redemption initiated event sent: {} for case {}", 
                    allocation.getCouponCode(), caseId);

        } catch (Exception e) {
            log.error("Error sending redemption initiated event: {}", e.getMessage(), e);
        }
    }

    /**
     * Send general supervisor coupon event
     */
    public void sendSupervisorCouponEvent(String eventType, Map<String, Object> eventData) {
        try {
            Map<String, Object> event = new HashMap<>(eventData);
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("supervisor.coupon.events", event);
            log.info("Supervisor coupon event sent: {}", eventType);

        } catch (Exception e) {
            log.error("Error sending supervisor coupon event: {}", e.getMessage(), e);
        }
    }
}