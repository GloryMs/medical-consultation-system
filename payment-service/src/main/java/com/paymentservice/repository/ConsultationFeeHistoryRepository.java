package com.paymentservice.repository;

import com.paymentservice.entity.ConsultationFeeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultationFeeHistoryRepository extends JpaRepository<ConsultationFeeHistory, Long> {
    
    List<ConsultationFeeHistory> findBySpecializationOrderByCreatedAtDesc(String specialization);
    
    List<ConsultationFeeHistory> findByChangedByOrderByCreatedAtDesc(Long changedBy);
    
    List<ConsultationFeeHistory> findByEffectiveDateBetween(LocalDateTime start, LocalDateTime end);
    
    Page<ConsultationFeeHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}