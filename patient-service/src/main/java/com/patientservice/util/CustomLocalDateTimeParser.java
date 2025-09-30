package com.patientservice.util;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomLocalDateTimeParser {

    /**
     * Parses a LocalDateTime from a string in the format "[year, month, day, hour, minute, second, nanosecond]".
     * Nanoseconds are optional.
     *
     * @param dateTimeString The string to parse, e.g., "[2025, 9, 27, 5, 3, 14, 769602792]"
     * @return A LocalDateTime object.
     * @throws IllegalArgumentException if the string format is invalid or has insufficient components.
     */
    public static LocalDateTime parseCustomFormat(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty.");
        }

        // Remove leading/trailing brackets and split by comma, handling optional spaces
        String cleanedString = dateTimeString.substring(1, dateTimeString.length() - 1);
        List<Integer> components = Arrays.stream(cleanedString.split(",\\s*"))
                                        .map(Integer::parseInt)
                                        .collect(Collectors.toList());

        // Validate the number of components
        if (components.size() < 6) {
            throw new IllegalArgumentException("Insufficient components to parse LocalDateTime. Expected at least 6 (year, month, day, hour, minute, second), but got " + components.size() + ". String: " + dateTimeString);
        }

        int year = components.get(0);
        int month = components.get(1);
        int day = components.get(2);
        int hour = components.get(3);
        int minute = components.get(4);
        int second = components.get(5);
        int nano = 0; // Default to 0 if nanoseconds are not provided

        // Check if nanoseconds component is present
        if (components.size() > 6) {
            nano = components.get(6);
        }

        return LocalDateTime.of(year, month, day, hour, minute, second, nano);
    }

    public static void main(String[] args) {
        String feeSetAtStringWithNanos = "[2025, 9, 27, 5, 3, 14, 769602792]";
        String feeSetAtStringWithoutNanos = "[2025, 9, 27, 5, 3, 14]";
        String invalidString = "[2025, 9, 27, 5, 3]"; // Missing second

        System.out.println("--- Testing valid inputs ---");
        try {
            LocalDateTime dateTime1 = parseCustomFormat(feeSetAtStringWithNanos);
            System.out.println("Parsed with nanos: " + dateTime1); // Expected: 2025-09-27T05:03:14.769602792
        } catch (Exception e) {
            System.err.println("Error parsing \"" + feeSetAtStringWithNanos + "\": " + e.getMessage());
        }

        try {
            LocalDateTime dateTime2 = parseCustomFormat(feeSetAtStringWithoutNanos);
            System.out.println("Parsed without nanos: " + dateTime2); // Expected: 2025-09-27T05:03:14
        } catch (Exception e) {
            System.err.println("Error parsing \"" + feeSetAtStringWithoutNanos + "\": " + e.getMessage());
        }

        System.out.println("\n--- Testing invalid input ---");
        try {
            LocalDateTime dateTime3 = parseCustomFormat(invalidString);
            System.out.println("Parsed invalid: " + dateTime3);
        } catch (Exception e) {
            System.err.println("Error parsing \"" + invalidString + "\": " + e.getMessage());
        }
    }
}
