package com.paymentservice.service.stripe;

import com.paymentservice.config.StripeConfig;
import com.paymentservice.exception.StripePaymentException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Core Stripe Payment Gateway Service
 * Handles all direct interactions with Stripe API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentGateway {

    //TODO all below must be logged ot Database
    
    private final StripeConfig stripeConfig;
    
    /**
     * Create a payment intent for consultation fees
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, 
                                            Map<String, String> metadata, String idempotencyKey) 
            throws StripePaymentException {
        try {
            // Convert amount to cents (Stripe uses smallest currency unit)
            Long amountInCents = amount.multiply(new BigDecimal(100)).longValue();
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .putAllMetadata(metadata)
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params, 
                RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build());
            
            log.info("Created payment intent: {}", paymentIntent.getId());
            return paymentIntent;
            
        } catch (StripeException e) {
            log.error("Failed to create payment intent", e);
            throw new StripePaymentException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Confirm a payment intent
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) 
            throws StripePaymentException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(paymentMethodId)
                .build();
            
            PaymentIntent confirmedIntent = paymentIntent.confirm(params);
            log.info("Confirmed payment intent: {}", confirmedIntent.getId());
            return confirmedIntent;
            
        } catch (StripeException e) {
            log.error("Failed to confirm payment intent: {}", paymentIntentId, e);
            throw new StripePaymentException("Failed to confirm payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel a payment intent
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripePaymentException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent canceledIntent = paymentIntent.cancel();
            log.info("Canceled payment intent: {}", canceledIntent.getId());
            return canceledIntent;
            
        } catch (StripeException e) {
            log.error("Failed to cancel payment intent: {}", paymentIntentId, e);
            throw new StripePaymentException("Failed to cancel payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create or retrieve a Stripe customer
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Customer createOrRetrieveCustomer(String email, String name, Map<String, String> metadata) 
            throws StripePaymentException {
        try {
            // First, try to find existing customer by email
            CustomerListParams listParams = CustomerListParams.builder()
                .setEmail(email)
                .setLimit(1L)
                .build();
            
            CustomerCollection customers = Customer.list(listParams);
            
            if (!customers.getData().isEmpty()) {
                Customer existingCustomer = customers.getData().get(0);
                log.info("Retrieved existing customer: {}", existingCustomer.getId());
                return existingCustomer;
            }
            
            // Create new customer if not found
            CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putAllMetadata(metadata)
                .build();
            
            Customer newCustomer = Customer.create(createParams);
            log.info("Created new customer: {}", newCustomer.getId());
            return newCustomer;
            
        } catch (StripeException e) {
            log.error("Failed to create/retrieve customer for email: {}", email, e);
            throw new StripePaymentException("Failed to process customer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Attach a payment method to a customer
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public PaymentMethod attachPaymentMethod(String paymentMethodId, String customerId) 
            throws StripePaymentException {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            
            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build();
            
            PaymentMethod attachedMethod = paymentMethod.attach(params);
            log.info("Attached payment method {} to customer {}", paymentMethodId, customerId);
            return attachedMethod;
            
        } catch (StripeException e) {
            log.error("Failed to attach payment method {} to customer {}", paymentMethodId, customerId, e);
            throw new StripePaymentException("Failed to attach payment method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a refund for a charge
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Refund createRefund(String chargeId, BigDecimal amount, String reason, 
                               Map<String, String> metadata) throws StripePaymentException {
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setCharge(chargeId)
                .putAllMetadata(metadata);
            
            // If amount is provided, refund partial amount; otherwise, full refund
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                Long amountInCents = amount.multiply(new BigDecimal(100)).longValue();
                paramsBuilder.setAmount(amountInCents);
            }
            
            if (reason != null) {
                paramsBuilder.setReason(RefundCreateParams.Reason.valueOf(reason.toUpperCase()));
            }
            
            Refund refund = Refund.create(paramsBuilder.build());
            log.info("Created refund {} for charge {}", refund.getId(), chargeId);
            return refund;
            
        } catch (StripeException e) {
            log.error("Failed to create refund for charge: {}", chargeId, e);
            throw new StripePaymentException("Failed to create refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a transfer to a connected account (for doctor payouts)
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Transfer createTransfer(BigDecimal amount, String destinationAccountId, 
                                  String description, Map<String, String> metadata) 
            throws StripePaymentException {
        try {
            Long amountInCents = amount.multiply(new BigDecimal(100)).longValue();
            
            TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setDestination(destinationAccountId)
                .setDescription(description)
                .putAllMetadata(metadata)
                .build();
            
            Transfer transfer = Transfer.create(params);
            log.info("Created transfer {} to account {}", transfer.getId(), destinationAccountId);
            return transfer;
            
        } catch (StripeException e) {
            log.error("Failed to create transfer to account: {}", destinationAccountId, e);
            throw new StripePaymentException("Failed to create transfer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve a payment intent by ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripePaymentException {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Failed to retrieve payment intent: {}", paymentIntentId, e);
            throw new StripePaymentException("Failed to retrieve payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve a charge by ID
     */
    public Charge retrieveCharge(String chargeId) throws StripePaymentException {
        try {
            return Charge.retrieve(chargeId);
        } catch (StripeException e) {
            log.error("Failed to retrieve charge: {}", chargeId, e);
            throw new StripePaymentException("Failed to retrieve charge: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a payment link for a specific amount
     */
    @Retryable(value = StripeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public PaymentLink createPaymentLink(BigDecimal amount, String currency, String description,
                                        Map<String, String> metadata) throws StripePaymentException {
        try {
            // First create a price
            Long amountInCents = amount.multiply(new BigDecimal(100)).longValue();
            
            PriceCreateParams priceParams = PriceCreateParams.builder()
                .setCurrency(currency.toLowerCase())
                .setUnitAmount(amountInCents)
                .setProductData(
                    PriceCreateParams.ProductData.builder()
                        .setName(description)
                        .build()
                )
                .build();
            
            Price price = Price.create(priceParams);
            
            // Then create the payment link
            PaymentLinkCreateParams linkParams = PaymentLinkCreateParams.builder()
                .addLineItem(
                    PaymentLinkCreateParams.LineItem.builder()
                        .setPrice(price.getId())
                        .setQuantity(1L)
                        .build()
                )
                .putAllMetadata(metadata)
                .build();
            
            PaymentLink paymentLink = PaymentLink.create(linkParams);
            log.info("Created payment link: {}", paymentLink.getUrl());
            return paymentLink;
            
        } catch (StripeException e) {
            log.error("Failed to create payment link", e);
            throw new StripePaymentException("Failed to create payment link: " + e.getMessage(), e);
        }
    }
    
    /**
     * List payment methods for a customer
     */
    public PaymentMethodCollection listPaymentMethods(String customerId) throws StripePaymentException {
        try {
            PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
            
            return PaymentMethod.list(params);
            
        } catch (StripeException e) {
            log.error("Failed to list payment methods for customer: {}", customerId, e);
            throw new StripePaymentException("Failed to list payment methods: " + e.getMessage(), e);
        }
    }
}