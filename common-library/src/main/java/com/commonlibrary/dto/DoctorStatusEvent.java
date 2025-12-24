package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DoctorStatusEvent {
    private String doctorEmail;
    private String newStatus;
    private Boolean approved;
    private Long timestamp;
}