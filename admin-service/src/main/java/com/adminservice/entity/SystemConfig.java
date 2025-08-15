package com.adminservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;

    private String configType;

    private String description;
}
