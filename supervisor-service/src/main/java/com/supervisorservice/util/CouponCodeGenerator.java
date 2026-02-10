package com.supervisorservice.util;

import java.security.SecureRandom;

/**
 * Utility class for generating unique coupon codes
 * Format: MED-XXXXXXXXX (e.g., MED-A3BK7N9PQ)
 */
public class CouponCodeGenerator {
    
    private static final String PREFIX = "MED";
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluding confusing chars: 0,O,1,I
    private static final int CODE_LENGTH = 9;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Generate a unique coupon code
     * 
     * @return Coupon code in format MED-XXXXXXXXX
     */
    public static String generate() {
        StringBuilder code = new StringBuilder(PREFIX);
        code.append("-");
        
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }
        
        return code.toString();
    }
    
    /**
     * Generate a batch code
     * Format: BATCH-YYYYMMDD-XXXXX
     * 
     * @return Batch code
     */
    public static String generateBatchCode() {
        StringBuilder code = new StringBuilder("BATCH-");
        
        // Add timestamp
        code.append(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
        code.append("-");
        
        // Add random suffix
        for (int i = 0; i < 5; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }
        
        return code.toString();
    }
    
    /**
     * Validate coupon code format
     * 
     * @param code Coupon code to validate
     * @return true if valid format
     */
    public static boolean isValidFormat(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        // Check format: MED-XXXXXXXXX
        return code.matches("^MED-[A-Z2-9]{9}$");
    }
}
