package com.commonlibrary.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TimeSlot {

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean isAvailable = true;

    private String description; // e.g., "Morning Consultation", "Emergency Hours"

    public boolean isTimeInSlot(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    public boolean overlaps(TimeSlot other) {
        return this.dayOfWeek.equals(other.dayOfWeek) &&
                !(this.endTime.isBefore(other.startTime) || this.startTime.isAfter(other.endTime));
    }

    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}