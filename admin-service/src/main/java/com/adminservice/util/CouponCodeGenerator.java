package com.adminservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility class for generating unique coupon codes and batch codes.
 */
@Component
public class CouponCodeGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");

    /**
     * Generate a unique coupon code.
     * Format: MED-{YEAR}-{RANDOM}
     * Example: MED-2026-ABC123
     *
     * @return Generated coupon code
     */
    public String generateCouponCode() {
        String year = LocalDateTime.now().format(YEAR_FORMATTER);
        String random = generateRandomString(6);
        return String.format("MED-%s-%s", year, random);
    }

    /**
     * Generate a unique coupon code with custom prefix.
     * Format: {PREFIX}-{YEAR}-{RANDOM}
     *
     * @param prefix Custom prefix
     * @return Generated coupon code
     */
    public String generateCouponCode(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return generateCouponCode();
        }
        String year = LocalDateTime.now().format(YEAR_FORMATTER);
        String random = generateRandomString(6);
        return String.format("%s-%s-%s", prefix.toUpperCase(), year, random);
    }

    /**
     * Generate a unique batch code.
     * Format: BATCH-{YEAR}{MONTH}-{RANDOM}
     * Example: BATCH-202601-XY789
     *
     * @return Generated batch code
     */
    public String generateBatchCode() {
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(YEAR_FORMATTER) + now.format(MONTH_FORMATTER);
        String random = generateRandomString(5);
        return String.format("BATCH-%s-%s", yearMonth, random);
    }

    /**
     * Generate a unique batch code with custom prefix.
     * Format: {PREFIX}-{YEAR}{MONTH}-{RANDOM}
     *
     * @param prefix Custom prefix
     * @return Generated batch code
     */
    public String generateBatchCode(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return generateBatchCode();
        }
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(YEAR_FORMATTER) + now.format(MONTH_FORMATTER);
        String random = generateRandomString(5);
        return String.format("%s-%s-%s", prefix.toUpperCase(), yearMonth, random);
    }

    /**
     * Generate coupon codes for a batch.
     * Format: {BATCH_CODE}-{SEQUENCE}
     * Example: BATCH-202601-XY789-001
     *
     * @param batchCode The batch code
     * @param count Number of codes to generate
     * @return Array of generated coupon codes
     */
    public String[] generateBatchCouponCodes(String batchCode, int count) {
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = String.format("%s-%03d", batchCode, i + 1);
        }
        return codes;
    }

    /**
     * Generate coupon codes for a batch with random suffix.
     * Format: {BATCH_CODE}-{RANDOM}
     * Example: BATCH-202601-XY789-A1B2
     *
     * @param batchCode The batch code
     * @param count Number of codes to generate
     * @return Array of generated coupon codes
     */
    public String[] generateBatchCouponCodesRandom(String batchCode, int count) {
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = String.format("%s-%s", batchCode, generateRandomString(4));
        }
        return codes;
    }

    /**
     * Generate a random alphanumeric string.
     *
     * @param length Desired length
     * @return Random string
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Generate a UUID-based code (for high uniqueness requirements).
     *
     * @return UUID-based code
     */
    public String generateUUIDCode() {
        return UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
    }

    /**
     * Validate coupon code format.
     *
     * @param couponCode Code to validate
     * @return true if valid format
     */
    public boolean isValidCouponCodeFormat(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return false;
        }
        // Basic validation: alphanumeric with dashes, 10-30 characters
        return couponCode.matches("^[A-Z0-9\\-]{10,30}$");
    }

    /**
     * Validate batch code format.
     *
     * @param batchCode Code to validate
     * @return true if valid format
     */
    public boolean isValidBatchCodeFormat(String batchCode) {
        if (batchCode == null || batchCode.trim().isEmpty()) {
            return false;
        }
        // Basic validation: alphanumeric with dashes, 10-25 characters
        return batchCode.matches("^[A-Z0-9\\-]{10,25}$");
    }

    /**
     * Normalize coupon code (uppercase, trim).
     *
     * @param couponCode Code to normalize
     * @return Normalized code
     */
    public String normalizeCouponCode(String couponCode) {
        if (couponCode == null) {
            return null;
        }
        return couponCode.trim().toUpperCase();
    }
}