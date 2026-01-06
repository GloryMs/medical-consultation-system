package com.supervisorservice.repository;

import com.commonlibrary.entity.SupervisorVerificationStatus;
import com.supervisorservice.entity.MedicalSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MedicalSupervisor entity
 */
@Repository
public interface MedicalSupervisorRepository extends JpaRepository<MedicalSupervisor, Long> {
    
    /**
     * Find supervisor by user ID
     */
    Optional<MedicalSupervisor> findByUserId(Long userId);
    
    /**
     * Find supervisor by user ID and not deleted
     */
    Optional<MedicalSupervisor> findByUserIdAndIsDeletedFalse(Long userId);
    
    /**
     * Find supervisor by email
     */
    Optional<MedicalSupervisor> findByEmail(String email);
    
    /**
     * Find supervisor by email and not deleted
     */
    Optional<MedicalSupervisor> findByEmailAndIsDeletedFalse(String email);
    
    /**
     * Find all supervisors by verification status
     */
    List<MedicalSupervisor> findByVerificationStatusAndIsDeletedFalse(SupervisorVerificationStatus status);
    
    /**
     * Find all verified and available supervisors
     */
    @Query("SELECT s FROM MedicalSupervisor s WHERE s.verificationStatus = 'VERIFIED' " +
           "AND s.isAvailable = true AND s.isDeleted = false")
    List<MedicalSupervisor> findAllVerifiedAndAvailable();
    
    /**
     * Find supervisors pending verification
     */
    @Query("SELECT s FROM MedicalSupervisor s WHERE s.verificationStatus = 'PENDING' " +
           "AND s.isDeleted = false ORDER BY s.createdAt ASC")
    List<MedicalSupervisor> findPendingVerification();
    
    /**
     * Count active supervisors
     */
    @Query("SELECT COUNT(s) FROM MedicalSupervisor s WHERE s.verificationStatus = 'VERIFIED' " +
           "AND s.isAvailable = true AND s.isDeleted = false")
    Long countActiveSupervisors();
    
    /**
     * Find supervisors with available capacity (can add more patients)
     */
    @Query("SELECT s FROM MedicalSupervisor s WHERE s.verificationStatus = 'VERIFIED' " +
           "AND s.isAvailable = true AND s.isDeleted = false " +
           "AND (SELECT COUNT(a) FROM SupervisorPatientAssignment a " +
           "WHERE a.supervisor.id = s.id AND a.assignmentStatus = 'ACTIVE' " +
           "AND a.isDeleted = false) < s.maxPatientsLimit")
    List<MedicalSupervisor> findSupervisorsWithCapacity();
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if email exists excluding specific supervisor
     */
    boolean existsByEmailAndIdNot(String email, Long id);
    
    /**
     * Check if user ID exists
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Find all supervisors (not deleted)
     */
    List<MedicalSupervisor> findByIsDeletedFalse();
    
    /**
     * Search supervisors by name or email
     */
    @Query("SELECT s FROM MedicalSupervisor s WHERE s.isDeleted = false " +
           "AND (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(s.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(s.organizationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<MedicalSupervisor> searchSupervisors(@Param("searchTerm") String searchTerm);
    
    /**
     * Get supervisor statistics
     */
    @Query("SELECT s.verificationStatus, COUNT(s) FROM MedicalSupervisor s " +
           "WHERE s.isDeleted = false GROUP BY s.verificationStatus")
    List<Object[]> getVerificationStatusStatistics();
}