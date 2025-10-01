package com.doctorservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotAvailabilityDto {
    private LocalDateTime scheduledTime;
    private Integer duration;
    private boolean available;
    private String message;
    private LocalDateTime conflictingAppointmentTime;
}