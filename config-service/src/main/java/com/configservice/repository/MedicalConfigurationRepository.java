package com.configservice.repository;

import com.configservice.entity.MedicalConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalConfigurationRepository extends JpaRepository<MedicalConfiguration, Long> {
    List<MedicalConfiguration> findByConfigTypeAndIsActiveTrueOrderBySortOrder(String configType);
    Optional<MedicalConfiguration> findByCodeAndIsActiveTrue(String code);
}