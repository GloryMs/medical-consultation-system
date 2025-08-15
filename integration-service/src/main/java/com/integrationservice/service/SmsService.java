package com.integrationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsService {
    
    public Map<String, String> sendSms(String phoneNumber, String message) {
        // Simulate SMS sending - In production, integrate with Twilio, AWS SNS, etc.
        log.info("Sending SMS to {}: {}", phoneNumber, message);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "sent");
        response.put("messageId", "SMS_" + System.currentTimeMillis());
        response.put("phoneNumber", phoneNumber);
        response.put("message", message);
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        // In production, this would call actual SMS API like:
        // TwilioClient.sendSms(phoneNumber, message);
        
        return response;
    }
}