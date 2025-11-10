package com.paymentservice.service;

import com.paymentservice.entity.ConsultationFee;
import com.paymentservice.entity.ConsultationFeeHistory;
import com.paymentservice.repository.ConsultationFeeHistoryRepository;
import com.paymentservice.repository.ConsultationFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing consultation fees by specialization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationFeeService {
    
    private final ConsultationFeeRepository feeRepository;
    private final ConsultationFeeHistoryRepository feeHistoryRepository;
    private final SystemConfigurationService systemConfigService;
    
    /**
     * Set or update consultation fee for a specialization
     */
    @Transactional
    @CacheEvict(value = "consultationFees", key = "#specialization")
    public ConsultationFee setFeeForSpecialization(String specialization, BigDecimal feeAmount, 
                                                   Long adminId, String reason) {
        // Validate inputs
        if (feeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be negative");
        }
        
        // Get existing fee if any
        ConsultationFee existingFee = feeRepository
            .findBySpecializationAndIsActive(specialization, true)
            .orElse(null);
        
        // Create history record
        ConsultationFeeHistory history = ConsultationFeeHistory.builder()
            .specialization(specialization)
            .oldFee(existingFee != null ? existingFee.getFeeAmount() : null)
            .newFee(feeAmount)
            .changedBy(adminId)
            .changeReason(reason)
            .effectiveDate(LocalDateTime.now())
            .build();
        
        feeHistoryRepository.save(history);
        
        // Update or create fee
        ConsultationFee fee;
        if (existingFee != null) {
            // Deactivate old fee
            existingFee.setIsActive(false);
            feeRepository.save(existingFee);
            
            // Create new fee entry
            fee = ConsultationFee.builder()
                .specialization(specialization)
                .feeAmount(feeAmount)
                .currency("USD")
                .isActive(true)
                .effectiveDate(LocalDateTime.now())
                .createdBy(adminId)
                .build();
        } else {
            // Create new fee
            fee = ConsultationFee.builder()
                .specialization(specialization)
                .feeAmount(feeAmount)
                .currency("USD")
                .isActive(true)
                .effectiveDate(LocalDateTime.now())
                .createdBy(adminId)
                .build();
        }
        
        fee = feeRepository.save(fee);
        
        log.info("Consultation fee updated for specialization '{}': {} -> {}", 
                specialization, 
                existingFee != null ? existingFee.getFeeAmount() : "N/A", 
                feeAmount);
        
        return fee;
    }
    
    /**
     * Get consultation fee by specialization
     */
    @Cacheable(value = "consultationFees", key = "#specialization")
    public BigDecimal getFeeBySpecialization(String specialization) {
        return feeRepository
            .findBySpecializationAndIsActive(specialization, true)
            .map(ConsultationFee::getFeeAmount)
            .orElseGet(() -> {
                log.warn("No fee found for specialization '{}', using default", specialization);
                return systemConfigService.getDefaultConsultationFee();
            });
    }
    
    /**
     * Get applicable consultation fee
     * Checks for unified fee setting first, then specialization-specific
     */
    public BigDecimal getApplicableFee(String specialization) {
        // Check if unified fee is enabled
        if (systemConfigService.isUnifiedFeeEnabled()) {
            BigDecimal unifiedFee = systemConfigService.getUnifiedFeeAmount();
            log.debug("Using unified consultation fee: {}", unifiedFee);
            return unifiedFee;
        }
        
        // Get specialization-specific fee
        return getFeeBySpecialization(specialization);
    }
    
    /**
     * Update consultation fee
     */
    @Transactional
    @CacheEvict(value = "consultationFees", key = "#specialization")
    public ConsultationFee updateConsultationFee(String specialization, BigDecimal newFee, 
                                                Long adminId, String reason) {
        return setFeeForSpecialization(specialization, newFee, adminId, reason);
    }
    
    /**
     * Get fee history for a specialization
     */
    public List<ConsultationFeeHistory> getFeeHistory(String specialization) {
        return feeHistoryRepository.findBySpecializationOrderByCreatedAtDesc(specialization);
    }
    
    /**
     * Get all fee history with pagination
     */
    public Page<ConsultationFeeHistory> getAllFeeHistory(Pageable pageable) {
        return feeHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * Enable unified fee for all specializations
     */
    @Transactional
    @CacheEvict(value = "consultationFees", allEntries = true)
    public void enableUnifiedFee(BigDecimal unifiedFeeAmount, Long adminId) {
        systemConfigService.setUnifiedFeeEnabled(true, adminId);
        systemConfigService.setUnifiedFeeAmount(unifiedFeeAmount, adminId);
        
        log.info("Unified consultation fee enabled at {} by admin {}", unifiedFeeAmount, adminId);
    }
    
    /**
     * Disable unified fee (return to specialization-based fees)
     */
    @Transactional
    @CacheEvict(value = "consultationFees", allEntries = true)
    public void disableUnifiedFee(Long adminId) {
        systemConfigService.setUnifiedFeeEnabled(false, adminId);
        
        log.info("Unified consultation fee disabled by admin {}", adminId);
    }
    
    /**
     * Set unified fee amount (when enabled)
     */
    @Transactional
    @CacheEvict(value = "consultationFees", allEntries = true)
    public void setUnifiedFeeAmount(BigDecimal amount, Long adminId) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be negative");
        }
        
        systemConfigService.setUnifiedFeeAmount(amount, adminId);
        
        log.info("Unified consultation fee amount updated to {} by admin {}", amount, adminId);
    }
    
    /**
     * Get all active consultation fees
     */
    public List<ConsultationFee> getAllActiveFees() {
        return feeRepository.findAllActiveFeesOrdered();
    }
    
    /**
     * Get all specializations with configured fees
     */
    public List<String> getAllConfiguredSpecializations() {
        return feeRepository.findAllActiveSpecializations();
    }
    
    /**
     * Validate if fee exists for specialization
     */
    public boolean isFeeConfiguredForSpecialization(String specialization) {
        return feeRepository.findBySpecializationAndIsActive(specialization, true).isPresent();
    }
    
    /**
     * Bulk update fees for multiple specializations
     */
    @Transactional
    @CacheEvict(value = "consultationFees", allEntries = true)
    public void bulkUpdateFees(Map<String, BigDecimal> specializationFees, Long adminId, String reason) {
        for (Map.Entry<String, BigDecimal> entry : specializationFees.entrySet()) {
            setFeeForSpecialization(entry.getKey(), entry.getValue(), adminId, reason);
        }
        
        log.info("Bulk fee update completed for {} specializations by admin {}", 
                specializationFees.size(), adminId);
    }
    
    /**
     * Get fee statistics
     */
    public Map<String, Object> getFeeStatistics() {
        List<ConsultationFee> activeFees = getAllActiveFees();
        
        BigDecimal minFee = activeFees.stream()
            .map(ConsultationFee::getFeeAmount)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal maxFee = activeFees.stream()
            .map(ConsultationFee::getFeeAmount)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal avgFee = activeFees.stream()
            .map(ConsultationFee::getFeeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(activeFees.size(), 1)), 2, BigDecimal.ROUND_HALF_UP);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSpecializations", activeFees.size());
        stats.put("minFee", minFee);
        stats.put("maxFee", maxFee);
        stats.put("avgFee", avgFee);
        stats.put("unifiedFeeEnabled", systemConfigService.isUnifiedFeeEnabled());
        if (systemConfigService.isUnifiedFeeEnabled()) {
            stats.put("unifiedFeeAmount", systemConfigService.getUnifiedFeeAmount());
        }
        
        return stats;
    }
}