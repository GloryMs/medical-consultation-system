package com.commonlibrary.repository;
import com.commonlibrary.entity.Disease;
import com.commonlibrary.entity.Symptom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SymptomRepository extends JpaRepository<Symptom, Long> {
    List<Symptom> findByIsActiveTrueOrderByBodySystem();
    List<Symptom> findByBodySystemAndIsActiveTrueOrderByName(String bodySystem);
    Optional<Symptom> findByCodeAndIsActiveTrue(String code);
    List<Symptom> findByCodeInAndIsActiveTrue(Set<String> codes);
    List<Symptom> findByRelevantSpecializationsContaining(String specialization);
}