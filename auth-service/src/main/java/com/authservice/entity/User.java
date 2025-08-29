package com.authservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.entity.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String googleOAuthId;

    private LocalDateTime lastLogin;

    private String preferredLanguage;

    private String timeZone;

    @Column(nullable = false)
    private Boolean isDeleted = false;

}
