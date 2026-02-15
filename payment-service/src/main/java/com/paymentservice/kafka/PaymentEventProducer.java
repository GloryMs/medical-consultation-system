package com.paymentservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationType;
import com.commonlibrary.entity.UserType;
import com.paymentservice.entity.Payment;
import com.paymentservice.entity.Subscription;
import com.paymentservice.feign.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    // Topic names
    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed-topic";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String PAYMENT_CANCELLED_TOPIC = "payment-cancelled";
    private static final String REFUND_PROCESSED_TOPIC = "refund-processed";
    private static final String SUBSCRIPTION_CREATED_TOPIC = "subscription-created";
    private static final String SUBSCRIPTION_UPDATED_TOPIC = "subscription-updated";
    private static final String SUBSCRIPTION_CANCELLED_TOPIC = "subscription-cancelled";
    private static final String PAYOUT_PROCESSED_TOPIC = "payout-processed";
    private static final String CONSULTATION_FEE_UPDATED_TOPIC = "consultation-fee-updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PatientServiceClient patientServiceClient;

    public void sendPaymentCompletedEvent(Long patientId, Long doctorId, Long caseId,
                                          String paymentType, Double amount, String transactionId) {
        // Send notification to notification service
        NotificationDto notification = NotificationDto.builder()
                .senderUserId(0L)
                .receiverUserId(getPatientUserId(patientId))
                .senderType(UserType.SYSTEM)
                .receiverType(UserType.PATIENT)
                .title("Payment Successful")
                .message("Your " + paymentType + " payment of $" + amount + " has been processed successfully. Transaction ID: " + transactionId)
                .type(NotificationType.PAYMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Payment completion notification sent for patient: {}", patientId);

        // Send payment event for other services
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("patientId", patientId);
        paymentEvent.put("doctorId", doctorId);
        paymentEvent.put("caseId", caseId);
        paymentEvent.put("paymentType", paymentType);
        paymentEvent.put("amount", amount);
        paymentEvent.put("transactionId", transactionId);
        paymentEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, paymentEvent);
        log.info("Payment event sent for case: {}", caseId);
    }

    /**
     * Send payment completed event
     */
    public void sendPaymentCompletedEvent(Payment payment) {

        // Send notification to notification service
        NotificationDto notification = NotificationDto.builder()
                .senderUserId(0L)
                .receiverUserId(getPatientUserId(payment.getPatientId()))
                .senderType(UserType.SYSTEM)
                .receiverType(UserType.PATIENT)
                .title("Payment Successful")
                .message("Your " + payment.getPaymentType().toString() + " payment of $" +
                        payment.getAmount() + " has been processed successfully. Transaction ID: " +
                        payment.getTransactionId())
                .type(NotificationType.PAYMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Payment completion notification sent for patient: {}", payment.getPatientId());


        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PAYMENT_COMPLETED");
        event.put("timestamp", LocalDateTime.now());
        event.put("paymentId", payment.getId());
        event.put("transactionId", payment.getTransactionId());
        event.put("patientId", payment.getPatientId());
        event.put("doctorId", payment.getDoctorId());
        event.put("caseId", payment.getCaseId());
        event.put("appointmentId", payment.getAppointmentId());
        event.put("amount", payment.getAmount());
        event.put("platformFee", payment.getPlatformFee());
        event.put("doctorAmount", payment.getDoctorAmount());
        event.put("paymentType", payment.getPaymentType().toString());
        event.put("paymentMethod", payment.getPaymentMethod().toString());
        event.put("stripePaymentIntentId", payment.getStripePaymentIntentId());

        kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, event);
        log.info("Sent payment completed event for payment ID: {}", payment.getId());
    }

    /**
     * Send payment failed event
     */
    public void sendPaymentFailedEvent(Payment payment) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PAYMENT_FAILED");
        event.put("timestamp", LocalDateTime.now());
        event.put("paymentId", payment.getId());
        event.put("patientId", payment.getPatientId());
        event.put("doctorId", payment.getDoctorId());
        event.put("caseId", payment.getCaseId());
        event.put("amount", payment.getAmount());
        event.put("failureReason", payment.getGatewayResponse());

        kafkaTemplate.send(PAYMENT_FAILED_TOPIC, event);
        log.info("Sent payment failed event for payment ID: {}", payment.getId());
    }

    /**
     * Send payment cancelled event
     */
    public void sendPaymentCancelledEvent(Payment payment) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PAYMENT_CANCELLED");
        event.put("timestamp", LocalDateTime.now());
        event.put("paymentId", payment.getId());
        event.put("patientId", payment.getPatientId());
        event.put("doctorId", payment.getDoctorId());
        event.put("caseId", payment.getCaseId());
        event.put("amount", payment.getAmount());

        kafkaTemplate.send(PAYMENT_CANCELLED_TOPIC, event);
        log.info("Sent payment cancelled event for payment ID: {}", payment.getId());
    }

    /**
     * Send refund processed event
     */
    public void sendRefundProcessedEvent(Payment payment, BigDecimal refundAmount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "REFUND_PROCESSED");
        event.put("timestamp", LocalDateTime.now());
        event.put("paymentId", payment.getId());
        event.put("patientId", payment.getPatientId());
        event.put("doctorId", payment.getDoctorId());
        event.put("caseId", payment.getCaseId());
        event.put("originalAmount", payment.getAmount());
        event.put("refundAmount", refundAmount);
        event.put("refundReason", payment.getRefundReason());
        event.put("stripeRefundId", payment.getStripeRefundId());

        kafkaTemplate.send(REFUND_PROCESSED_TOPIC, event);
        log.info("Sent refund processed event for payment ID: {}", payment.getId());
    }

    /**
     * Send subscription created event
     */
    public void sendSubscriptionCreatedEvent(Subscription subscription) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SUBSCRIPTION_CREATED");
        event.put("timestamp", LocalDateTime.now());
        event.put("subscriptionId", subscription.getId());
        event.put("userId", subscription.getUserId());
        event.put("userType", subscription.getUserType());
        event.put("planType", subscription.getPlanType());
        event.put("planDuration", subscription.getPlanDuration());
        event.put("amount", subscription.getAmount());
        event.put("status", subscription.getStatus());
        event.put("trialEnd", subscription.getTrialEnd());
        event.put("currentPeriodEnd", subscription.getCurrentPeriodEnd());
        event.put("stripeSubscriptionId", subscription.getStripeSubscriptionId());

        kafkaTemplate.send(SUBSCRIPTION_CREATED_TOPIC, event);
        log.info("Sent subscription created event for subscription ID: {}", subscription.getId());
    }

    /**
     * Send subscription updated event
     */
    public void sendSubscriptionUpdatedEvent(Subscription subscription, String updateType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SUBSCRIPTION_UPDATED");
        event.put("timestamp", LocalDateTime.now());
        event.put("subscriptionId", subscription.getId());
        event.put("userId", subscription.getUserId());
        event.put("userType", subscription.getUserType());
        event.put("updateType", updateType); // RENEWED, UPGRADED, DOWNGRADED, etc.
        event.put("planType", subscription.getPlanType());
        event.put("status", subscription.getStatus());
        event.put("currentPeriodEnd", subscription.getCurrentPeriodEnd());

        kafkaTemplate.send(SUBSCRIPTION_UPDATED_TOPIC, event);
        log.info("Sent subscription updated event for subscription ID: {}", subscription.getId());
    }

    /**
     * Send subscription cancelled event
     */
    public void sendSubscriptionCancelledEvent(Subscription subscription) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SUBSCRIPTION_CANCELLED");
        event.put("timestamp", LocalDateTime.now());
        event.put("subscriptionId", subscription.getId());
        event.put("userId", subscription.getUserId());
        event.put("userType", subscription.getUserType());
        event.put("planType", subscription.getPlanType());
        event.put("cancelledAt", subscription.getCanceledAt());
        event.put("cancellationReason", subscription.getCancellationReason());
        event.put("cancelAtPeriodEnd", subscription.isCancelAtPeriodEnd());

        kafkaTemplate.send(SUBSCRIPTION_CANCELLED_TOPIC, event);
        log.info("Sent subscription cancelled event for subscription ID: {}", subscription.getId());
    }

    /**
     * Send payout processed event
     */
    public void sendPayoutProcessedEvent(Long doctorId, BigDecimal amount, String transferId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PAYOUT_PROCESSED");
        event.put("timestamp", LocalDateTime.now());
        event.put("doctorId", doctorId);
        event.put("amount", amount);
        event.put("transferId", transferId);

        kafkaTemplate.send(PAYOUT_PROCESSED_TOPIC, event);
        log.info("Sent payout processed event for doctor ID: {}", doctorId);
    }

    /**
     * Send consultation fee updated event
     */
    public void sendConsultationFeeUpdatedEvent(String specialization, BigDecimal oldFee,
                                                BigDecimal newFee, Long adminId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CONSULTATION_FEE_UPDATED");
        event.put("timestamp", LocalDateTime.now());
        event.put("specialization", specialization);
        event.put("oldFee", oldFee);
        event.put("newFee", newFee);
        event.put("updatedBy", adminId);

        kafkaTemplate.send(CONSULTATION_FEE_UPDATED_TOPIC, event);
        log.info("Sent consultation fee updated event for specialization: {}", specialization);
    }

    private Long getPatientUserId(Long patientId) {
        Long patientUserId = null;
        try{
            patientUserId = patientServiceClient.getPatientUserId( patientId ).getBody().getData().getUserId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return patientUserId;
    }
}