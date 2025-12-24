package com.authservice.kafka;

import com.authservice.service.AuthService;
import com.commonlibrary.dto.DoctorStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final AuthService authService;

    @KafkaListener(topics = "auth-system-events-topic", groupId = "auth-group")
    public void handleDoctorUserAccountVerification(DoctorStatusEvent doctorStatusEvent) {
        log.info("handleDoctorUserAccountVerification:");
        try {
            log.info("Kafka Auth Listener: receiving verification changed status for doctor {}, to {}",
                    doctorStatusEvent.getDoctorEmail(), doctorStatusEvent.getNewStatus());

            authService.verifyDoctorUser( doctorStatusEvent.getDoctorEmail(), doctorStatusEvent.getNewStatus(),
                    doctorStatusEvent.getApproved() );

        } catch (Exception e) {
            log.error("Error processing verification changed status for doctor ..");
            e.printStackTrace();
        }
    }
}
