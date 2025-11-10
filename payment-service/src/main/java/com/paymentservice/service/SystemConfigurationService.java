package com.paymentservice.service;

import com.paymentservice.entity.SystemConfiguration;
import com.paymentservice.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for managing system-wide configuration parameters
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationService {
    
    private final SystemConfigurationRepository configRepository;
    
    // Configuration keys
    public static final String DOCTOR_TRIAL_PERIOD_DAYS = "doctor_trial_period_days";
    public static final String PLATFORM_FEE_PERCENTAGE = "platform_fee_percentage";
    public static final String USE_UNIFIED_CONSULTATION_FEE = "use_unified_consultation_fee";
    public static final String UNIFIED_CONSULTATION_FEE = "unified_consultation_fee";
    public static final String DEFAULT_CONSULTATION_FEE = "default_consultation_fee";
    
    @PostConstruct
    public void initializeDefaultConfigurations() {
        // Initialize default configurations if not exists
        createConfigIfNotExists(DOCTOR_TRIAL_PERIOD_DAYS, "90", "NUMBER", 
            "Free trial period for doctors in days");
        
        createConfigIfNotExists(PLATFORM_FEE_PERCENTAGE, "0.10", "NUMBER", 
            "Platform fee percentage for consultations");
        
        createConfigIfNotExists(USE_UNIFIED_CONSULTATION_FEE, "true", "BOOLEAN",
            "Use single fee for all specializations");
        
        createConfigIfNotExists(UNIFIED_CONSULTATION_FEE, "200.00", "NUMBER",
            "Unified fee amount if enabled");
        
        createConfigIfNotExists(DEFAULT_CONSULTATION_FEE, "200.00", "NUMBER",
            "Default consultation fee when specialization fee not found");
    }
    
    private void createConfigIfNotExists(String key, String value, String type, String description) {
        if (!configRepository.existsByConfigKey(key)) {
            SystemConfiguration config = SystemConfiguration.builder()
                .configKey(key)
                .configValue(value)
                .configType(type)
                .description(description)
                .isActive(true)
                .build();
            configRepository.save(config);
            log.info("Created default configuration: {} = {}", key, value);
        }
    }
    
    @Cacheable(value = "systemConfig", key = "#key")
    public String getConfigValue(String key) {
        return configRepository.findByConfigKeyAndIsActive(key, true)
            .map(SystemConfiguration::getConfigValue)
            .orElseThrow(() -> new IllegalStateException("Configuration not found: " + key));
    }
    
    @Transactional
    @CacheEvict(value = "systemConfig", key = "#key")
    public void updateConfigValue(String key, String value, Long updatedBy) {
        SystemConfiguration config = configRepository.findByConfigKeyAndIsActive(key, true)
            .orElseThrow(() -> new IllegalStateException("Configuration not found: " + key));
        
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(LocalDateTime.now());
        
        configRepository.save(config);
        log.info("Updated configuration: {} = {} by user {}", key, value, updatedBy);
    }
    
    // Convenience methods for specific configurations
    
    public Integer getDoctorTrialPeriod() {
        return Integer.parseInt(getConfigValue(DOCTOR_TRIAL_PERIOD_DAYS));
    }
    
    public void setDoctorTrialPeriod(Integer days, Long updatedBy) {
        updateConfigValue(DOCTOR_TRIAL_PERIOD_DAYS, days.toString(), updatedBy);
    }
    
    public BigDecimal getPlatformFeePercentage() {
        return new BigDecimal(getConfigValue(PLATFORM_FEE_PERCENTAGE));
    }
    
    public void setPlatformFeePercentage(BigDecimal percentage, Long updatedBy) {
        updateConfigValue(PLATFORM_FEE_PERCENTAGE, percentage.toString(), updatedBy);
    }
    
    public boolean isUnifiedFeeEnabled() {
        return Boolean.parseBoolean(getConfigValue(USE_UNIFIED_CONSULTATION_FEE));
    }
    
    public void setUnifiedFeeEnabled(boolean enabled, Long updatedBy) {
        updateConfigValue(USE_UNIFIED_CONSULTATION_FEE, String.valueOf(enabled), updatedBy);
    }
    
    public BigDecimal getUnifiedFeeAmount() {
        return new BigDecimal(getConfigValue(UNIFIED_CONSULTATION_FEE));
    }
    
    public void setUnifiedFeeAmount(BigDecimal amount, Long updatedBy) {
        updateConfigValue(UNIFIED_CONSULTATION_FEE, amount.toString(), updatedBy);
    }
    
    public BigDecimal getDefaultConsultationFee() {
        return new BigDecimal(getConfigValue(DEFAULT_CONSULTATION_FEE));
    }
    
    public void setDefaultConsultationFee(BigDecimal amount, Long updatedBy) {
        updateConfigValue(DEFAULT_CONSULTATION_FEE, amount.toString(), updatedBy);
    }
}