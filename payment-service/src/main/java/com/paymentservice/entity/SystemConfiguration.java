package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * System Configuration entity for managing configurable parameters
 */
@Entity
@Table(name = "system_configurations", indexes = {
        @Index(name = "idx_config_key", columnList = "config_key", unique = true),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_type", length = 50)
    private String configType; // STRING, NUMBER, BOOLEAN, JSON

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}