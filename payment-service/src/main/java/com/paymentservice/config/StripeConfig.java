package com.paymentservice.config;

import com.stripe.Stripe;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import jakarta.annotation.PostConstruct;

/**
 * Stripe Payment Gateway Configuration
 * Initializes Stripe SDK with API keys and configuration settings
 */
@Configuration
@EnableRetry
@Slf4j
@Getter
public class StripeConfig {
    
    @Value("${stripe.api.secret-key}")
    private String secretKey;
    
    @Value("${stripe.api.publishable-key}")
    private String publishableKey;
    
    @Value("${stripe.api.webhook-secret}")
    private String webhookSecret;
    
    @Value("${stripe.connect.platform-account-id}")
    private String platformAccountId;
    
    @Value("${stripe.settings.platform-fee-percentage}")
    private Double platformFeePercentage;
    
    @Value("${stripe.settings.max-retry-attempts}")
    private Integer maxRetryAttempts;
    
    @Value("${stripe.settings.connect-timeout}")
    private Integer connectTimeout;
    
    @Value("${stripe.settings.read-timeout}")
    private Integer readTimeout;
    
    // Product and Price IDs
    @Value("${stripe.products.patient.basic.product-id:}")
    private String patientBasicProductId;
    
    @Value("${stripe.products.patient.basic.price-id:}")
    private String patientBasicPriceId;
    
    @Value("${stripe.products.patient.premium.six-month-price-id:}")
    private String patientPremium6MonthPriceId;
    
    @Value("${stripe.products.patient.premium.yearly-price-id:}")
    private String patientPremiumYearlyPriceId;
    
    @Value("${stripe.products.patient.pro.six-month-price-id:}")
    private String patientPro6MonthPriceId;
    
    @Value("${stripe.products.patient.pro.yearly-price-id:}")
    private String patientProYearlyPriceId;
    
    @Value("${stripe.products.doctor.pro.product-id:}")
    private String doctorProProductId;
    
    @Value("${stripe.products.doctor.pro.yearly-price-id:}")
    private String doctorProYearlyPriceId;
    
    @PostConstruct
    public void init() {
        try {
            // Initialize Stripe with secret key
            Stripe.apiKey = secretKey;
            
            // Set additional Stripe configuration
            Stripe.setMaxNetworkRetries(maxRetryAttempts);
            Stripe.setConnectTimeout(connectTimeout);
            Stripe.setReadTimeout(readTimeout);
            
            log.info("Stripe SDK initialized successfully");
            log.info("Platform Account ID: {}", platformAccountId);
            log.info("Platform Fee Percentage: {}%", platformFeePercentage * 100);
            
            // Validate critical configuration
            validateConfiguration();
            
        } catch (Exception e) {
            log.error("Failed to initialize Stripe configuration", e);
            throw new RuntimeException("Stripe initialization failed", e);
        }
    }
    
    private void validateConfiguration() {
        if (secretKey == null || secretKey.isEmpty() || secretKey.contains("your_test_key")) {
            throw new IllegalStateException("Stripe secret key is not properly configured");
        }
        
        if (webhookSecret == null || webhookSecret.isEmpty() || webhookSecret.contains("your_webhook_secret")) {
            log.warn("Stripe webhook secret is not configured. Webhook signature validation will fail.");
        }
        
        if (platformFeePercentage < 0 || platformFeePercentage > 1) {
            throw new IllegalStateException("Platform fee percentage must be between 0 and 1");
        }
    }
    
    /**
     * Get platform fee amount for a given total
     */
    public Long calculatePlatformFee(Long amountInCents) {
        return Math.round(amountInCents * platformFeePercentage);
    }
    
    /**
     * Get doctor amount after platform fee
     */
    public Long calculateDoctorAmount(Long amountInCents) {
        return amountInCents - calculatePlatformFee(amountInCents);
    }
}