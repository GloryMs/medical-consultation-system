package com.paymentservice.service.stripe;

import com.paymentservice.config.StripeConfig;
import com.paymentservice.entity.Subscription;
import com.paymentservice.entity.SubscriptionPlan;
import com.paymentservice.exception.StripePaymentException;
import com.paymentservice.repository.SubscriptionPlanRepository;
import com.paymentservice.repository.SubscriptionRepository;
import com.paymentservice.service.SystemConfigurationService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing Stripe subscriptions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSubscriptionService {
    
    private final StripeConfig stripeConfig;
    private final StripePaymentGateway paymentGateway;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SystemConfigurationService systemConfigService;
    
    /**
     * Create a doctor subscription with yearly plan and trial period
     */
    @Transactional
    public Subscription createDoctorSubscription(Long doctorId, String email, String name,
                                                String paymentMethodId) throws StripePaymentException {
        try {
            // Get trial period from configuration
            Integer trialDays = systemConfigService.getDoctorTrialPeriod();
            
            // Check if doctor already has a subscription
            Optional<Subscription> existingSubscription = 
                subscriptionRepository.findActiveByUserIdAndUserType(doctorId, "DOCTOR");
            if (existingSubscription.isPresent()) {
                throw new StripePaymentException("Doctor already has an active subscription");
            }
            
            // Create or retrieve Stripe customer
            Map<String, String> metadata = new HashMap<>();
            metadata.put("user_id", String.valueOf(doctorId));
            metadata.put("user_type", "DOCTOR");
            Customer customer = paymentGateway.createOrRetrieveCustomer(email, name, metadata);
            
            // Attach payment method if provided
            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                paymentGateway.attachPaymentMethod(paymentMethodId, customer.getId());
                
                // Set as default payment method
                CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                        CustomerUpdateParams.InvoiceSettings.builder()
                            .setDefaultPaymentMethod(paymentMethodId)
                            .build()
                    )
                    .build();
                customer.update(updateParams);
            }
            
            // Get doctor subscription plan
            SubscriptionPlan plan = subscriptionPlanRepository
                .findActivePlan("DOCTOR", "PRO", 12)
                .orElseThrow(() -> new StripePaymentException("Doctor subscription plan not found"));
            
            // Create Stripe subscription with trial
            SubscriptionCreateParams.Builder subscriptionParamsBuilder = 
                SubscriptionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .addItem(
                        SubscriptionCreateParams.Item.builder()
                            .setPrice(plan.getStripePriceId())
                            .build()
                    )
                    .setTrialPeriodDays(Long.valueOf(trialDays))
                    .putMetadata("doctor_id", String.valueOf(doctorId));
            
            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                subscriptionParamsBuilder.setDefaultPaymentMethod(paymentMethodId);
            }
            
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.create(subscriptionParamsBuilder.build());
            
            // Save subscription to database
            Subscription subscription = Subscription.builder()
                .userId(doctorId)
                .userType("DOCTOR")
                .stripeSubscriptionId(stripeSubscription.getId())
                .stripeCustomerId(customer.getId())
                .planType("PRO")
                .planDuration(12)
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .status(stripeSubscription.getStatus())
                .trialPeriodDays(trialDays)
                .trialStart(convertTimestampToLocalDateTime(stripeSubscription.getTrialStart()))
                .trialEnd(convertTimestampToLocalDateTime(stripeSubscription.getTrialEnd()))
                .currentPeriodStart(convertTimestampToLocalDateTime(stripeSubscription.getCurrentPeriodStart()))
                .currentPeriodEnd(convertTimestampToLocalDateTime(stripeSubscription.getCurrentPeriodEnd()))
                .stripePriceId(plan.getStripePriceId())
                .stripePaymentMethodId(paymentMethodId)
                .build();
            
            subscription.addMetadata("stripe_customer_name", name);
            subscription.addMetadata("stripe_customer_email", email);
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to create doctor subscription for ID: {}", doctorId, e);
            throw new StripePaymentException("Failed to create doctor subscription: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a patient subscription (6-month or yearly)
     */
    @Transactional
    public Subscription createPatientSubscription(Long patientId, String email, String name,
                                                 String planType, Integer durationMonths,
                                                 String paymentMethodId) throws StripePaymentException {
        try {
            // Validate plan type
            if (!"BASIC".equals(planType) && !"PREMIUM".equals(planType) && !"PRO".equals(planType)) {
                throw new StripePaymentException("Invalid plan type: " + planType);
            }
            
            // Basic plan is free, no Stripe subscription needed
            if ("BASIC".equals(planType)) {
                return createFreeBasicSubscription(patientId);
            }
            
            // Validate duration (6 or 12 months only)
            if (durationMonths != 6 && durationMonths != 12) {
                throw new StripePaymentException("Invalid duration. Must be 6 or 12 months");
            }
            
            // Check for existing active subscription
            Optional<Subscription> existingSubscription = 
                subscriptionRepository.findActiveByUserIdAndUserType(patientId, "PATIENT");
            if (existingSubscription.isPresent()) {
                throw new StripePaymentException("Patient already has an active subscription");
            }
            
            // Get subscription plan
            SubscriptionPlan plan = subscriptionPlanRepository
                .findActivePlan("PATIENT", planType, durationMonths)
                .orElseThrow(() -> new StripePaymentException("Subscription plan not found"));
            
            // Create or retrieve Stripe customer
            Map<String, String> metadata = new HashMap<>();
            metadata.put("user_id", String.valueOf(patientId));
            metadata.put("user_type", "PATIENT");
            Customer customer = paymentGateway.createOrRetrieveCustomer(email, name, metadata);
            
            // Attach and set payment method
            if (paymentMethodId == null || paymentMethodId.isEmpty()) {
                throw new StripePaymentException("Payment method is required for paid subscriptions");
            }
            
            paymentGateway.attachPaymentMethod(paymentMethodId, customer.getId());
            
            CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build()
                )
                .build();
            customer.update(updateParams);
            
            // Create Stripe subscription (no trial for patients)
            SubscriptionCreateParams subscriptionParams = SubscriptionCreateParams.builder()
                .setCustomer(customer.getId())
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(plan.getStripePriceId())
                        .build()
                )
                .setDefaultPaymentMethod(paymentMethodId)
                .putMetadata("patient_id", String.valueOf(patientId))
                .putMetadata("plan_type", planType)
                .putMetadata("duration_months", String.valueOf(durationMonths))
                .build();
            
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.create(subscriptionParams);
            
            // Save subscription to database
            Subscription subscription = Subscription.builder()
                .userId(patientId)
                .userType("PATIENT")
                .stripeSubscriptionId(stripeSubscription.getId())
                .stripeCustomerId(customer.getId())
                .planType(planType)
                .planDuration(durationMonths)
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .status(stripeSubscription.getStatus())
                .currentPeriodStart(convertTimestampToLocalDateTime(stripeSubscription.getCurrentPeriodStart()))
                .currentPeriodEnd(convertTimestampToLocalDateTime(stripeSubscription.getCurrentPeriodEnd()))
                .stripePriceId(plan.getStripePriceId())
                .stripePaymentMethodId(paymentMethodId)
                .build();
            
            subscription.addMetadata("stripe_customer_name", name);
            subscription.addMetadata("stripe_customer_email", email);
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to create patient subscription for ID: {}", patientId, e);
            throw new StripePaymentException("Failed to create patient subscription: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a free basic subscription (no Stripe involved)
     */
    private Subscription createFreeBasicSubscription(Long patientId) {
        Subscription subscription = Subscription.builder()
            .userId(patientId)
            .userType("PATIENT")
            .planType("BASIC")
            .planDuration(0) // Unlimited
            .amount(BigDecimal.ZERO)
            .currency("USD")
            .status("active")
            .currentPeriodStart(LocalDateTime.now())
            .currentPeriodEnd(LocalDateTime.now().plusYears(100)) // Effectively unlimited
            .build();
        
        subscription.addMetadata("free_tier", "true");
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Cancel a subscription
     */
    @Transactional
    public Subscription cancelSubscription(Long subscriptionId, boolean immediately, String reason) 
            throws StripePaymentException {
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new StripePaymentException("Subscription not found"));
            
            // If it's a free basic plan, just update status
            if ("BASIC".equals(subscription.getPlanType())) {
                subscription.setStatus("canceled");
                subscription.setCanceledAt(LocalDateTime.now());
                subscription.setCancellationReason(reason);
                return subscriptionRepository.save(subscription);
            }
            
            // Cancel Stripe subscription
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
            
            if (immediately) {
                // Cancel immediately
                SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                    .setInvoiceNow(true)
                    .setProrate(true)
                    .build();
                stripeSubscription = stripeSubscription.cancel(params);
            } else {
                // Cancel at period end
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();
                stripeSubscription = stripeSubscription.update(params);
            }
            
            // Update database
            subscription.setStatus(stripeSubscription.getStatus());
            subscription.setCancelAtPeriodEnd(!immediately);
            if (immediately) {
                subscription.setCanceledAt(LocalDateTime.now());
            }
            subscription.setCancellationReason(reason);
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to cancel subscription ID: {}", subscriptionId, e);
            throw new StripePaymentException("Failed to cancel subscription: " + e.getMessage(), e);
        }
    }
    
    /**
     * Pause a subscription
     */
    @Transactional
    public Subscription pauseSubscription(Long subscriptionId) throws StripePaymentException {
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new StripePaymentException("Subscription not found"));
            
            if ("BASIC".equals(subscription.getPlanType())) {
                throw new StripePaymentException("Cannot pause a free basic subscription");
            }
            
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
            
            // Pause the subscription
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setPauseCollection(
                    SubscriptionUpdateParams.PauseCollection.builder()
                        .setBehavior(SubscriptionUpdateParams.PauseCollection.Behavior.MARK_UNCOLLECTIBLE)
                        .build()
                )
                .build();
            
            stripeSubscription = stripeSubscription.update(params);
            
            // Update database
            subscription.setStatus("paused");
            subscription.addMetadata("paused_at", LocalDateTime.now().toString());
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to pause subscription ID: {}", subscriptionId, e);
            throw new StripePaymentException("Failed to pause subscription: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resume a paused subscription
     */
    @Transactional
    public Subscription resumeSubscription(Long subscriptionId) throws StripePaymentException {
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new StripePaymentException("Subscription not found"));
            
            if (!"paused".equals(subscription.getStatus())) {
                throw new StripePaymentException("Subscription is not paused");
            }
            
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
            
            // Resume the subscription
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setPauseCollection(SubscriptionUpdateParams.PauseCollection.builder().build())
                .build();
            
            stripeSubscription = stripeSubscription.update(params);
            
            // Update database
            subscription.setStatus(stripeSubscription.getStatus());
            subscription.addMetadata("resumed_at", LocalDateTime.now().toString());
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to resume subscription ID: {}", subscriptionId, e);
            throw new StripePaymentException("Failed to resume subscription: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update subscription payment method
     */
    @Transactional
    public Subscription updatePaymentMethod(Long subscriptionId, String newPaymentMethodId) 
            throws StripePaymentException {
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new StripePaymentException("Subscription not found"));
            
            if ("BASIC".equals(subscription.getPlanType())) {
                throw new StripePaymentException("Basic subscription doesn't require payment method");
            }
            
            // Attach payment method to customer
            paymentGateway.attachPaymentMethod(newPaymentMethodId, subscription.getStripeCustomerId());
            
            // Update subscription's default payment method
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
            
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setDefaultPaymentMethod(newPaymentMethodId)
                .build();
            
            stripeSubscription.update(params);
            
            // Update database
            subscription.setStripePaymentMethodId(newPaymentMethodId);
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to update payment method for subscription ID: {}", subscriptionId, e);
            throw new StripePaymentException("Failed to update payment method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sync subscription status from Stripe
     */
    @Transactional
    public Subscription syncSubscriptionStatus(String stripeSubscriptionId) throws StripePaymentException {
        try {
            Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new StripePaymentException("Subscription not found"));
            
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            
            // Update subscription status and dates
            subscription.setStatus(stripeSubscription.getStatus());
            subscription.setCurrentPeriodStart(convertTimestampToLocalDateTime(stripeSubscription.getCurrentPeriodStart()));
            subscription.setCurrentPeriodEnd(convertTimestampToLocalDateTime(stripeSubscription.getCurrentPeriodEnd()));
            
            if (stripeSubscription.getTrialStart() != null) {
                subscription.setTrialStart(convertTimestampToLocalDateTime(stripeSubscription.getTrialStart()));
            }
            if (stripeSubscription.getTrialEnd() != null) {
                subscription.setTrialEnd(convertTimestampToLocalDateTime(stripeSubscription.getTrialEnd()));
            }
            
            subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());
            
            if (stripeSubscription.getCanceledAt() != null) {
                subscription.setCanceledAt(convertTimestampToLocalDateTime(stripeSubscription.getCanceledAt()));
            }
            
            return subscriptionRepository.save(subscription);
            
        } catch (StripeException e) {
            log.error("Failed to sync subscription: {}", stripeSubscriptionId, e);
            throw new StripePaymentException("Failed to sync subscription: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to convert timestamp to LocalDateTime
     */
    private LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }
}