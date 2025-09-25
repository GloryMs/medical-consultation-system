package com.patientservice.repository;

import com.patientservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT D FROM Document D where D.medicalCase.id= ?1")
    List<Document> findByCaseId(Long caseId);

//    @Query("SELECT D FROM Document D where D.id= ?1 AND D.medicalCase.id= ?2")
//    Optional<Document> findByIdAndCaseId(Long id, Long caseId);
}
