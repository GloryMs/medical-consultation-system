package com.patientservice.repository;

import com.patientservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT D FROM Document D where D.medicalCase.id= ?1")
    List<Document> findByCaseId(Long caseId);

}
