package com.commonlibrary.dto;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MedicalConfigurationDto{

    private String configType;
    private String code;
    private String name;
    private String description;
    private String parentCode;
    private Integer level;
    private Boolean isActive;
    private Integer sortOrder;
    private String attributes;
}