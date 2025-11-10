package com.paymentservice.controller;

import com.paymentservice.config.StripeConfig;
import com.paymentservice.service.PaymentProcessingService;
import com.paymentservice.service.stripe.StripeSubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling Stripe webhooks
 */
@RestController
@RequestMapping("/api/stripe/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final StripeConfig stripeConfig;
    private final PaymentProcessingService paymentProcessingService;
    private final StripeSubscriptionService subscriptionService;
    
    /**
     * Handle Stripe webhook events
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;
        
        try {
            // Verify webhook signature
            event = Webhook.constructEvent(
                payload, 
                sigHeader, 
                stripeConfig.getWebhookSecret()
            );
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }
        
        // Handle the event
        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;
                
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event);
                break;
                
            case "payment_intent.canceled":
                handlePaymentIntentCanceled(event);
                break;
                
            case "charge.refunded":
                handleChargeRefunded(event);
                break;
                
            case "customer.subscription.created":
                handleSubscriptionCreated(event);
                break;
                
            case "customer.subscription.updated":
                handleSubscriptionUpdated(event);
                break;
                
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
                
            case "customer.subscription.trial_will_end":
                handleSubscriptionTrialWillEnd(event);
                break;
                
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
                
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
                
            default:
                log.info("Unhandled event type: {}", event.getType());
        }
        
        return ResponseEntity.ok("Event processed");
    }
    
    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        log.info("Payment intent succeeded: {}", paymentIntent.getId());
        
        paymentProcessingService.handlePaymentIntentWebhook(
            paymentIntent.getId(), 
            "succeeded"
        );
    }
    
    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        log.info("Payment intent failed: {}", paymentIntent.getId());
        
        paymentProcessingService.handlePaymentIntentWebhook(
            paymentIntent.getId(), 
            "payment_failed"
        );
    }
    
    private void handlePaymentIntentCanceled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        log.info("Payment intent canceled: {}", paymentIntent.getId());
        
        paymentProcessingService.handlePaymentIntentWebhook(
            paymentIntent.getId(), 
            "canceled"
        );
    }
    
    private void handleChargeRefunded(Event event) {
        Charge charge = (Charge) event.getData().getObject();
        log.info("Charge refunded: {}", charge.getId());
        
        // Handle refund confirmation
        if (charge.getRefunded()) {
            log.info("Full refund processed for charge: {}", charge.getId());
        } else if (charge.getAmountRefunded() > 0) {
            log.info("Partial refund of {} processed for charge: {}", 
                charge.getAmountRefunded(), charge.getId());
        }
    }
    
    private void handleSubscriptionCreated(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription created: {}", subscription.getId());
        
        try {
            subscriptionService.syncSubscriptionStatus(subscription.getId());
        } catch (Exception e) {
            log.error("Failed to sync subscription: {}", subscription.getId(), e);
        }
    }
    
    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription updated: {}", subscription.getId());
        
        try {
            subscriptionService.syncSubscriptionStatus(subscription.getId());
        } catch (Exception e) {
            log.error("Failed to sync subscription: {}", subscription.getId(), e);
        }
    }
    
    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription deleted: {}", subscription.getId());
        
        try {
            subscriptionService.syncSubscriptionStatus(subscription.getId());
        } catch (Exception e) {
            log.error("Failed to sync subscription: {}", subscription.getId(), e);
        }
    }
    
    private void handleSubscriptionTrialWillEnd(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Subscription trial will end: {}", subscription.getId());
        
        // TODO: Send notification to user about trial ending
    }
    
    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getData().getObject();
        log.info("Invoice payment succeeded: {}", invoice.getId());
        
        // Handle successful subscription renewal
        if (invoice.getSubscription() != null) {
            try {
                subscriptionService.syncSubscriptionStatus(invoice.getSubscription());
            } catch (Exception e) {
                log.error("Failed to sync subscription from invoice: {}", invoice.getId(), e);
            }
        }
    }
    
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getData().getObject();
        log.info("Invoice payment failed: {}", invoice.getId());
        
        // TODO: Send notification about failed payment
        // TODO: Handle subscription suspension if needed
    }
}