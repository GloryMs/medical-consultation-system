package com.doctorservice.repository;

import com.doctorservice.entity.DoctorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DoctorDocument entity
 */
@Repository
public interface DoctorDocumentRepository extends JpaRepository<DoctorDocument, Long> {

    /**
     * Find all documents for a specific doctor
     */
    List<DoctorDocument> findByDoctorId(Long doctorId);

    /**
     * Find documents by doctor ID and document type
     */
    List<DoctorDocument> findByDoctorIdAndDocumentType(Long doctorId, DoctorDocument.DocumentType documentType);

    /**
     * Find a specific document by ID and doctor ID (for access control)
     */
    Optional<DoctorDocument> findByIdAndDoctorId(Long documentId, Long doctorId);

    /**
     * Count documents by doctor ID
     */
    long countByDoctorId(Long doctorId);

    /**
     * Count verified documents for a doctor
     */
    @Query("SELECT COUNT(d) FROM DoctorDocument d WHERE d.doctor.id = :doctorId AND d.verifiedByAdmin = true")
    long countVerifiedByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Check if doctor has a specific document type
     */
    boolean existsByDoctorIdAndDocumentType(Long doctorId, DoctorDocument.DocumentType documentType);

    /**
     * Find all unverified documents
     */
    List<DoctorDocument> findByVerifiedByAdminFalse();

    /**
     * Delete all documents for a doctor
     */
    void deleteByDoctorId(Long doctorId);

    /**
     * Check if doctor has all required documents (LICENSE and CERTIFICATE)
     */
    @Query("SELECT CASE WHEN COUNT(DISTINCT d.documentType) >= 2 THEN true ELSE false END " +
           "FROM DoctorDocument d " +
           "WHERE d.doctor.id = :doctorId " +
           "AND d.documentType IN ('LICENSE', 'CERTIFICATE')")
    boolean hasAllRequiredDocuments(@Param("doctorId") Long doctorId);

    /**
     * Find all documents for doctors pending verification
     */
    @Query("SELECT d FROM DoctorDocument d " +
           "WHERE d.doctor.verificationStatus = 'PENDING' " +
           "AND d.verifiedByAdmin = false " +
           "ORDER BY d.uploadedAt ASC")
    List<DoctorDocument> findPendingVerificationDocuments();
}