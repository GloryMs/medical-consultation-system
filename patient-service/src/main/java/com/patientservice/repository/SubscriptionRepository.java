package com.patientservice.repository;

import com.patientservice.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByPatientId(Long patientId);
    Optional<Subscription> findTopByPatientIdOrderByCreatedAtDesc(Long patientId);
}