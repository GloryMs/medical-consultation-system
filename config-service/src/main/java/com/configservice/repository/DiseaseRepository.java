package com.configservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseRepository extends JpaRepository<com.configservice.entity.Disease, Long> {
    List<com.configservice.entity.Disease> findByIsActiveTrueOrderByCategory();
    List<com.configservice.entity.Disease> findByCategoryAndIsActiveTrueOrderByName(String category);
    Optional<com.configservice.entity.Disease> findByIcdCodeAndIsActiveTrue(String icdCode);
    List<com.configservice.entity.Disease> findByRequiredSpecializationsContaining(String specialization);
    boolean existsByIcdCode(String icdCode);
    @Query("SELECT D FROM Disease D WHERE D.icdCode = :icdCode")
    Optional<com.configservice.entity.Disease> findDiseaseByIcdCodeCustom(@Param ("icdCode") String icdCode);
}