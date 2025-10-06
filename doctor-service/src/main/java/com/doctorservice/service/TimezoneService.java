package com.doctorservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling timezone conversions across global doctors and patients
 */
@Service
@Slf4j
public class TimezoneService {

    // Comprehensive country to timezone mapping
    private static final Map<String, String> COUNTRY_TIMEZONE_MAP = new HashMap<>();
    
    static {
        // Europe
        COUNTRY_TIMEZONE_MAP.put("DE", "Europe/Berlin");
        COUNTRY_TIMEZONE_MAP.put("GERMANY", "Europe/Berlin");
        COUNTRY_TIMEZONE_MAP.put("GB", "Europe/London");
        COUNTRY_TIMEZONE_MAP.put("UK", "Europe/London");
        COUNTRY_TIMEZONE_MAP.put("FR", "Europe/Paris");
        COUNTRY_TIMEZONE_MAP.put("FRANCE", "Europe/Paris");
        COUNTRY_TIMEZONE_MAP.put("ES", "Europe/Madrid");
        COUNTRY_TIMEZONE_MAP.put("SPAIN", "Europe/Madrid");
        COUNTRY_TIMEZONE_MAP.put("IT", "Europe/Rome");
        COUNTRY_TIMEZONE_MAP.put("ITALY", "Europe/Rome");
        COUNTRY_TIMEZONE_MAP.put("NL", "Europe/Amsterdam");
        COUNTRY_TIMEZONE_MAP.put("NETHERLANDS", "Europe/Amsterdam");
        COUNTRY_TIMEZONE_MAP.put("BE", "Europe/Brussels");
        COUNTRY_TIMEZONE_MAP.put("BELGIUM", "Europe/Brussels");
        COUNTRY_TIMEZONE_MAP.put("CH", "Europe/Zurich");
        COUNTRY_TIMEZONE_MAP.put("SWITZERLAND", "Europe/Zurich");
        COUNTRY_TIMEZONE_MAP.put("AT", "Europe/Vienna");
        COUNTRY_TIMEZONE_MAP.put("AUSTRIA", "Europe/Vienna");
        COUNTRY_TIMEZONE_MAP.put("SE", "Europe/Stockholm");
        COUNTRY_TIMEZONE_MAP.put("SWEDEN", "Europe/Stockholm");
        COUNTRY_TIMEZONE_MAP.put("NO", "Europe/Oslo");
        COUNTRY_TIMEZONE_MAP.put("NORWAY", "Europe/Oslo");
        COUNTRY_TIMEZONE_MAP.put("DK", "Europe/Copenhagen");
        COUNTRY_TIMEZONE_MAP.put("DENMARK", "Europe/Copenhagen");
        COUNTRY_TIMEZONE_MAP.put("PL", "Europe/Warsaw");
        COUNTRY_TIMEZONE_MAP.put("POLAND", "Europe/Warsaw");
        
        // Americas
        COUNTRY_TIMEZONE_MAP.put("US", "America/New_York"); // Default to Eastern
        COUNTRY_TIMEZONE_MAP.put("USA", "America/New_York");
        COUNTRY_TIMEZONE_MAP.put("CA", "America/Toronto");
        COUNTRY_TIMEZONE_MAP.put("CANADA", "America/Toronto");
        COUNTRY_TIMEZONE_MAP.put("MX", "America/Mexico_City");
        COUNTRY_TIMEZONE_MAP.put("MEXICO", "America/Mexico_City");
        COUNTRY_TIMEZONE_MAP.put("BR", "America/Sao_Paulo");
        COUNTRY_TIMEZONE_MAP.put("BRAZIL", "America/Sao_Paulo");
        COUNTRY_TIMEZONE_MAP.put("AR", "America/Argentina/Buenos_Aires");
        COUNTRY_TIMEZONE_MAP.put("ARGENTINA", "America/Argentina/Buenos_Aires");
        COUNTRY_TIMEZONE_MAP.put("CL", "America/Santiago");
        COUNTRY_TIMEZONE_MAP.put("CHILE", "America/Santiago");
        
        // Asia
        COUNTRY_TIMEZONE_MAP.put("CN", "Asia/Shanghai");
        COUNTRY_TIMEZONE_MAP.put("CHINA", "Asia/Shanghai");
        COUNTRY_TIMEZONE_MAP.put("JP", "Asia/Tokyo");
        COUNTRY_TIMEZONE_MAP.put("JAPAN", "Asia/Tokyo");
        COUNTRY_TIMEZONE_MAP.put("IN", "Asia/Kolkata");
        COUNTRY_TIMEZONE_MAP.put("INDIA", "Asia/Kolkata");
        COUNTRY_TIMEZONE_MAP.put("KR", "Asia/Seoul");
        COUNTRY_TIMEZONE_MAP.put("SOUTH_KOREA", "Asia/Seoul");
        COUNTRY_TIMEZONE_MAP.put("SG", "Asia/Singapore");
        COUNTRY_TIMEZONE_MAP.put("SINGAPORE", "Asia/Singapore");
        COUNTRY_TIMEZONE_MAP.put("TH", "Asia/Bangkok");
        COUNTRY_TIMEZONE_MAP.put("THAILAND", "Asia/Bangkok");
        COUNTRY_TIMEZONE_MAP.put("MY", "Asia/Kuala_Lumpur");
        COUNTRY_TIMEZONE_MAP.put("MALAYSIA", "Asia/Kuala_Lumpur");
        COUNTRY_TIMEZONE_MAP.put("PH", "Asia/Manila");
        COUNTRY_TIMEZONE_MAP.put("PHILIPPINES", "Asia/Manila");
        COUNTRY_TIMEZONE_MAP.put("VN", "Asia/Ho_Chi_Minh");
        COUNTRY_TIMEZONE_MAP.put("VIETNAM", "Asia/Ho_Chi_Minh");
        COUNTRY_TIMEZONE_MAP.put("ID", "Asia/Jakarta");
        COUNTRY_TIMEZONE_MAP.put("INDONESIA", "Asia/Jakarta");
        
        // Middle East
        COUNTRY_TIMEZONE_MAP.put("AE", "Asia/Dubai");
        COUNTRY_TIMEZONE_MAP.put("UAE", "Asia/Dubai");
        COUNTRY_TIMEZONE_MAP.put("SA", "Asia/Riyadh");
        COUNTRY_TIMEZONE_MAP.put("SAUDI_ARABIA", "Asia/Riyadh");
        COUNTRY_TIMEZONE_MAP.put("IL", "Asia/Jerusalem");
        COUNTRY_TIMEZONE_MAP.put("ISRAEL", "Asia/Jerusalem");
        COUNTRY_TIMEZONE_MAP.put("TR", "Europe/Istanbul");
        COUNTRY_TIMEZONE_MAP.put("TURKEY", "Europe/Istanbul");
        
        // Oceania
        COUNTRY_TIMEZONE_MAP.put("AU", "Australia/Sydney");
        COUNTRY_TIMEZONE_MAP.put("AUSTRALIA", "Australia/Sydney");
        COUNTRY_TIMEZONE_MAP.put("NZ", "Pacific/Auckland");
        COUNTRY_TIMEZONE_MAP.put("NEW_ZEALAND", "Pacific/Auckland");
        
        // Africa
        COUNTRY_TIMEZONE_MAP.put("ZA", "Africa/Johannesburg");
        COUNTRY_TIMEZONE_MAP.put("SOUTH_AFRICA", "Africa/Johannesburg");
        COUNTRY_TIMEZONE_MAP.put("EG", "Africa/Cairo");
        COUNTRY_TIMEZONE_MAP.put("EGYPT", "Africa/Cairo");
        COUNTRY_TIMEZONE_MAP.put("NG", "Africa/Lagos");
        COUNTRY_TIMEZONE_MAP.put("NIGERIA", "Africa/Lagos");
        COUNTRY_TIMEZONE_MAP.put("KE", "Africa/Nairobi");
        COUNTRY_TIMEZONE_MAP.put("KENYA", "Africa/Nairobi");
    }

    /**
     * Get timezone for a country code or name
     */
    public String getTimezoneForCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return "UTC";
        }
        
        String timezone = COUNTRY_TIMEZONE_MAP.get(country.toUpperCase().trim());
        return timezone != null ? timezone : "UTC";
    }

    /**
     * Convert time from one timezone to another
     */
    public LocalDateTime convertTime(LocalDateTime sourceTime, 
                                     String sourceTimezone, 
                                     String targetTimezone) {
        ZoneId sourceZone = ZoneId.of(sourceTimezone);
        ZoneId targetZone = ZoneId.of(targetTimezone);
        
        ZonedDateTime sourceZonedTime = sourceTime.atZone(sourceZone);
        ZonedDateTime targetZonedTime = sourceZonedTime.withZoneSameInstant(targetZone);
        
        return targetZonedTime.toLocalDateTime();
    }

    /**
     * Format datetime with timezone information
     */
    public String formatWithTimezone(LocalDateTime dateTime, String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime zonedDateTime = dateTime.atZone(zone);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
            "MMM dd, yyyy 'at' hh:mm a z");
        
        return zonedDateTime.format(formatter);
    }

    /**
     * Get timezone abbreviation (e.g., CET, EST, PST)
     */
    public String getTimezoneAbbreviation(String timezoneId) {
        try {
            ZoneId zone = ZoneId.of(timezoneId);
            ZonedDateTime now = ZonedDateTime.now(zone);
            return zone.getDisplayName(
                java.time.format.TextStyle.SHORT, 
                java.util.Locale.ENGLISH
            );
        } catch (Exception e) {
            log.warn("Could not get abbreviation for timezone: {}", timezoneId);
            return timezoneId;
        }
    }

    /**
     * Calculate time difference in hours between two timezones
     */
    public int getTimezoneDifferenceHours(String timezone1, String timezone2) {
        ZoneId zone1 = ZoneId.of(timezone1);
        ZoneId zone2 = ZoneId.of(timezone2);
        
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime time1 = now.withZoneSameInstant(zone1);
        ZonedDateTime time2 = now.withZoneSameInstant(zone2);
        
        long diffSeconds = Duration.between(time2, time1).getSeconds();
        return (int) (diffSeconds / 3600);
    }

    /**
     * Validate timezone ID
     */
    public boolean isValidTimezone(String timezoneId) {
        try {
            ZoneId.of(timezoneId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current time in a specific timezone
     */
    public LocalDateTime getCurrentTimeInTimezone(String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        return LocalDateTime.now(zone);
    }

    /**
     * Check if appointment time is during reasonable hours in recipient's timezone
     * (e.g., not in the middle of the night)
     */
    public boolean isReasonableHour(LocalDateTime appointmentTime, String timezone) {
        int hour = appointmentTime.getHour();
        // Consider 7 AM to 10 PM as reasonable hours
        return hour >= 7 && hour <= 22;
    }
}