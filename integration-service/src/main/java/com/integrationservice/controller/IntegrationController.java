package com.integrationservice.controller;

import com.integrationservice.dto.SmsRequestDto;
import com.integrationservice.service.SmsService;
import com.integrationservice.service.WhatsAppIntegrationService;
import com.integrationservice.service.ZoomIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final ZoomIntegrationService zoomService;
    private final WhatsAppIntegrationService whatsAppService;
    private final SmsService smsService;

    @PostMapping("/zoom/meeting")
    public ResponseEntity<Map<String, String>> createZoomMeeting(
            @RequestParam String topic,
            @RequestParam String startTime) {
        Map<String, String> meeting = zoomService.createMeeting(topic, startTime);
        return ResponseEntity.ok(meeting);
    }

    @PostMapping("/whatsapp/send")
    public ResponseEntity<Void> sendWhatsAppMessage(
            @RequestParam String phoneNumber,
            @RequestParam String message) {
        whatsAppService.sendMessage(phoneNumber, message);
        return ResponseEntity.ok().build();
    }

    // 41. Send SMS Notification - MISSING ENDPOINT
    @PostMapping("/sms/send")
    public ResponseEntity<Map<String, String>> sendSmsNotification(
            @Valid @RequestBody SmsRequestDto dto) {
        Map<String, String> result = smsService.sendSms(dto.getPhoneNumber(), dto.getMessage());
        return ResponseEntity.ok(result);
    }
}
