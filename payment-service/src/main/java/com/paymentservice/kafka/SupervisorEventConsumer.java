package com.paymentservice.kafka;

import com.paymentservice.entity.Payment;
import com.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Consumes events from supervisor-service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorEventConsumer {
    
    private final PaymentRepository paymentRepository;
    
    /**
     * Handle coupon redeemed event
     */
    @KafkaListener(topics = "supervisor.coupon.redeemed", groupId = "payment-service-group")
    public void handleCouponRedeemed(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            String couponCode = event.get("couponCode").toString();
            BigDecimal discountAmount = new BigDecimal(event.get("discountAmount").toString());
            
            log.info("Coupon {} redeemed for case {} - discount: ${}", 
                    couponCode, caseId, discountAmount);
            
            // Record coupon usage in payment history (optional)
            // This helps with reconciliation and reporting
            
        } catch (Exception e) {
            log.error("Error handling coupon redeemed event", e);
        }
    }
    
    /**
     * Handle coupon expired event
     */
    @KafkaListener(topics = "supervisor.coupon.expired", groupId = "payment-service-group")
    public void handleCouponExpired(Map<String, Object> event) {
        try {
            String couponCode = event.get("couponCode").toString();
            Long patientId = Long.valueOf(event.get("patientId").toString());
            
            log.info("Coupon {} expired for patient {}", couponCode, patientId);
            
            // Update any pending payments that were expecting this coupon
            
        } catch (Exception e) {
            log.error("Error handling coupon expired event", e);
        }
    }
}