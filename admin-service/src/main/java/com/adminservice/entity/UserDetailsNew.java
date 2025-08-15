package com.adminservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "users_details")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsNew extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    private String role;

    private String status;

    private LocalDateTime lastLogin;
}
