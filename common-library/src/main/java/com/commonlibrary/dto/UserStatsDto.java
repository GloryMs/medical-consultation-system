package com.commonlibrary.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    Long totalUsers;
    Long activeUsers;
    Long pendingDoctorUsers;
    Long pendingSupervisorUsers;
    Long doctors;
    Long patients;
    Long admins;
    Long supervisors;
}
