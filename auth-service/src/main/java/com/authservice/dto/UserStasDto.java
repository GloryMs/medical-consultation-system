package com.authservice.dto;

import jakarta.persistence.SequenceGenerators;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStasDto {
    Long totalUsers;
    Long activeUsers;
    Long pendingUsers;
    Long doctors;
    Long patients;
    Long admins;
}
