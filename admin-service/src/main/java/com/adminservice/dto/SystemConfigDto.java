package com.adminservice.dto;

import lombok.Data;

@Data
public class SystemConfigDto {
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
}