package com.integrationservice.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ZoomIntegrationService {

    public Map<String, String> createMeeting(String topic, String startTime) {
        // Simulate Zoom meeting creation
        Map<String, String> meeting = new HashMap<>();
        meeting.put("meetingId", "ZOOM_" + System.currentTimeMillis());
        meeting.put("joinUrl", "https://zoom.us/j/" + System.currentTimeMillis());
        meeting.put("password", "123456");
        meeting.put("topic", topic);
        meeting.put("startTime", startTime);
        return meeting;
    }
}
