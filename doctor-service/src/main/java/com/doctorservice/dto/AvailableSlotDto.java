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
public class AvailableSlotDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private boolean available;
}

