package com.paymentservice.repository;

import com.paymentservice.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {
    
    Optional<SystemConfiguration> findByConfigKey(String configKey);
    
    Optional<SystemConfiguration> findByConfigKeyAndIsActive(String configKey, boolean isActive);
    
    boolean existsByConfigKey(String configKey);
}