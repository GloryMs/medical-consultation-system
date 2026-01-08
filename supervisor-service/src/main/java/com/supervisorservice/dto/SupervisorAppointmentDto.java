package com.supervisorservice.dto;

import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.dto.DoctorDto;
import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.entity.ConsultationType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for appointment information in supervisor context
 * Extends base appointment data with supervisor-specific fields
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorAppointmentDto {

    private Long id;
    private Long caseId;
    private String caseTitle;
    private DoctorDto doctor;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private BigDecimal consultationFee;
    private LocalDateTime scheduledTime;
    private Integer duration;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private Integer rescheduleCount;
    private String meetingLink;
    private String meetingId;
    private LocalDateTime rescheduledFrom;
    private String rescheduleReason;
    private LocalDateTime completedAt;
    
    // Supervisor-specific fields
    private Long supervisorId;
    private Boolean isSupervisorManaged;
    private Boolean hasPendingRescheduleRequest;
    private Long rescheduleRequestId;
    private String specialization;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method to create from AppointmentDto
     */
    public static SupervisorAppointmentDto fromAppointmentDto(AppointmentDto appointmentDto) {
        if (appointmentDto == null) {
            return null;
        }
        
        SupervisorAppointmentDto dto = new SupervisorAppointmentDto();
        dto.setId(appointmentDto.getId());
        dto.setCaseId(appointmentDto.getCaseId());
        dto.setDoctor(appointmentDto.getDoctor());
        dto.setPatientId(appointmentDto.getPatientId());
        dto.setPatientName(appointmentDto.getPatientName());
        dto.setConsultationFee(appointmentDto.getConsultationFee());
        dto.setScheduledTime(appointmentDto.getScheduledTime());
        dto.setDuration(appointmentDto.getDuration());
        dto.setConsultationType(appointmentDto.getConsultationType());
        dto.setStatus(appointmentDto.getStatus());
        dto.setRescheduleCount(appointmentDto.getRescheduleCount());
        dto.setMeetingLink(appointmentDto.getMeetingLink());
        dto.setMeetingId(appointmentDto.getMeetingId());
        dto.setRescheduledFrom(appointmentDto.getRescheduledFrom());
        dto.setRescheduleReason(appointmentDto.getRescheduleReason());
        dto.setCompletedAt(appointmentDto.getCompletedAt());
        
        // Extract doctor info if available
        if (appointmentDto.getDoctor() != null) {
            dto.setDoctorId(appointmentDto.getDoctor().getId());
            dto.setDoctorName(appointmentDto.getDoctor().getFullName());
            dto.setSpecialization(appointmentDto.getDoctor().getPrimarySpecialization());
        }
        
        return dto;
    }
}