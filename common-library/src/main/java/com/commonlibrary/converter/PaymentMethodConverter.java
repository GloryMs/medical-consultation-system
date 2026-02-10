package com.commonlibrary.converter;

import com.commonlibrary.entity.PaymentMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Custom converter to handle PaymentMethod enum conversion
 * Supports both numeric (legacy) and string values
 */
@Converter(autoApply = true)
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        // Store as string value
        return paymentMethod.name();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        // Try to parse as enum name first (for new string values)
        try {
            return PaymentMethod.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If it fails, try to parse as numeric value (for legacy data)
            try {
                int numericValue = Integer.parseInt(dbData);
                return convertNumericToEnum(numericValue);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                    "Cannot convert database value '" + dbData + "' to PaymentMethod enum", ex);
            }
        }
    }

    /**
     * Maps legacy numeric values to enum constants
     * Adjust these mappings based on your database's numeric values
     */
    private PaymentMethod convertNumericToEnum(int value) {
        switch (value) {
            case 1:
                return PaymentMethod.CREDIT_CARD;
            case 2:
                return PaymentMethod.PAYPAL;
            case 3:
                return PaymentMethod.BANK_TRANSFER;
            case 4:
                return PaymentMethod.STRIPE;
            default:
                throw new IllegalArgumentException(
                    "No PaymentMethod enum constant for numeric value: " + value);
        }
    }
}