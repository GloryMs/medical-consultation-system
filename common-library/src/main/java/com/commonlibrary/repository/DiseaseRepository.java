package com.commonlibrary.repository;

import com.commonlibrary.entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long> {
    List<Disease> findByIsActiveTrueOrderByCategory();
    List<Disease> findByCategoryAndIsActiveTrueOrderByName(String category);
    Optional<Disease> findByIcdCodeAndIsActiveTrue(String icdCode);
    List<Disease> findByRequiredSpecializationsContaining(String specialization);
    boolean existsByIcdCode(String icdCode);
    @Query("SELECT D FROM Disease D WHERE D.icdCode = :icdCode")
    Optional<Disease> findDiseaseByIcdCodeCustom(@Param ("icdCode") String icdCode);
}