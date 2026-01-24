package com.supervisorservice.service;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.feign.NotificationServiceClient;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class SupervisorNotificationService {
    private final NotificationServiceClient notificationServiceClient;
    private final MedicalSupervisorRepository supervisorRepository;


    public List<NotificationDto> getMyNotifications(Long userId){
        MedicalSupervisor supervisor = supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
        List<NotificationDto> dtos = new ArrayList<>();
        try{
            dtos = notificationServiceClient.getUserNotifications(supervisor.getId()).getBody().getData();
        } catch (Exception e) {
            log.error("Failed to get supervisor (with userId {}) notifications {} ", userId, e.getMessage());
        }
        return dtos;
    }

    public void markAsRead(Long notificationId, Long userId){
        MedicalSupervisor supervisor = supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
        try{
            notificationServiceClient.markAsRead(notificationId ,supervisor.getUserId());
        } catch (Exception e) {
            log.error("Failed to mark notification as read, error: {}", e.getMessage());
        }
    }

    public void markAllAsRead(Long userId){
        MedicalSupervisor supervisor = supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
        try{
            notificationServiceClient.markAllAsRead(supervisor.getUserId());
        } catch (Exception e) {
            log.error("Failed to mark all notifications as read, error: {}", e.getMessage());
        }
    }

}
