package com.integrationservice.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppIntegrationService {

    public void sendMessage(String phoneNumber, String message) {
        // Simulate WhatsApp message sending
        System.out.println("Sending WhatsApp message to " + phoneNumber + ": " + message);
    }

    public String generateWhatsAppLink(String phoneNumber) {
        return "https://wa.me/" + phoneNumber;
    }
}
