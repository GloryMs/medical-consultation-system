package com.supervisorservice.repository;

import com.commonlibrary.entity.SupervisorAssignmentStatus;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorPatientAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SupervisorPatientAssignment entity
 */
@Repository
public interface SupervisorPatientAssignmentRepository extends JpaRepository<SupervisorPatientAssignment, Long> {
    
    /**
     * Find assignment by supervisor ID and patient ID
     */
    Optional<SupervisorPatientAssignment> findBySupervisorIdAndPatientId(Long supervisorId, Long patientId);

    /**
     * Find assignment by patient ID and status
     */
    Optional<SupervisorPatientAssignment> findByPatientIdAndAssignmentStatus(
            Long patientId,
            SupervisorAssignmentStatus status);

    /**
     * Check if assignment exists by supervisor ID and patient ID (any status)
     * This checks for both active and inactive assignments to prevent duplicate assignments
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
            "AND a.patientId = :patientId AND a.isDeleted = false")
    boolean existsByAssignmentKey(@Param("supervisorId") Long supervisorId, @Param("patientId") Long patientId);


    List<SupervisorPatientAssignment> findActiveAssignmentsBySupervisor(MedicalSupervisor supervisor);
    /**
     * Find active assignment by supervisor ID and patient ID
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
           "AND a.patientId = :patientId AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    Optional<SupervisorPatientAssignment> findActiveAssignment(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId);
    
    /**
     * Find all assignments for a supervisor
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
           "AND a.isDeleted = false ORDER BY a.assignedAt DESC")
    List<SupervisorPatientAssignment> findBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find active assignments for a supervisor
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
           "AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false ORDER BY a.assignedAt DESC")
    List<SupervisorPatientAssignment> findActiveBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find all assignments for a patient
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.patientId = :patientId " +
           "AND a.isDeleted = false ORDER BY a.assignedAt DESC")
    List<SupervisorPatientAssignment> findByPatientId(@Param("patientId") Long patientId);
    
    /**
     * Find active assignment for a patient
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.patientId = :patientId " +
           "AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    Optional<SupervisorPatientAssignment> findActiveByPatientId(@Param("patientId") Long patientId);
    
    /**
     * Count active assignments for a supervisor
     */
//    @Query("SELECT COUNT(a.id) FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
//           "AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
//    long countActiveAssignmentsBySupervisor(@Param("supervisorId") Long supervisorId);

    @Query("SELECT COUNT(s) FROM SupervisorPatientAssignment s WHERE s.supervisor.id = :supervisorId AND" +
            " s.assignmentStatus = 'ACTIVE'")
    Long countActiveAssignmentsBySupervisor(@Param("supervisorId") Long supervisorId);
    
    /**
     * Check if active assignment exists
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
           "AND a.patientId = :patientId AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    boolean existsActiveAssignment(@Param("supervisorId") Long supervisorId, @Param("patientId") Long patientId);
    
    /**
     * Find all patients assigned to a supervisor
     */
    @Query("SELECT DISTINCT a.patientId FROM SupervisorPatientAssignment a " +
           "WHERE a.supervisor.id = :supervisorId AND a.assignmentStatus = 'ACTIVE' " +
           "AND a.isDeleted = false")
    List<Long> findPatientIdsBySupervisor(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find assignments by status
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
           "AND a.assignmentStatus = :status AND a.isDeleted = false ORDER BY a.assignedAt DESC")
    List<SupervisorPatientAssignment> findBySupervisorIdAndStatus(
            @Param("supervisorId") Long supervisorId,
            @Param("status") SupervisorAssignmentStatus status);
    
    /**
     * Check if patient has any active supervisor assignment
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM SupervisorPatientAssignment a WHERE a.patientId = :patientId " +
           "AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    boolean patientHasActiveSupervisor(@Param("patientId") Long patientId);
    
    /**
     * Find all assignments by supervisor user ID
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.supervisor.userId = :userId " +
           "AND a.isDeleted = false ORDER BY a.assignedAt DESC")
    List<SupervisorPatientAssignment> findBySupervisorUserId(@Param("userId") Long userId);


//    /**
//     * Find supervisor by user ID
//     */
//    Optional<MedicalSupervisor> findByUserId(Long userId);
//
//    /**
//     * Find supervisor by user ID and not deleted
//     */
//    Optional<MedicalSupervisor> findByUserIdAndIsDeletedFalse(Long userId);
//
//    /**
//     * Check if supervisor exists by user ID
//     */
//    boolean existsByUserId(Long userId);
//
//    /**
//     * Find supervisor by email
//     */
//    Optional<MedicalSupervisor> findByEmailAndIsDeletedFalse(String email);
//
//
//    /**
//     * Find all active supervisors
//     */
//    List<MedicalSupervisor> findByIsAvailableTrueAndIsDeletedFalse();
//
//    /**
//     * Count supervisors by verification status
//     */
//    long countByVerificationStatusAndIsDeletedFalse(SupervisorVerificationStatus status);
//
//    /**
//     * Find supervisors by organization
//     */
//    List<MedicalSupervisor> findByOrganizationNameAndIsDeletedFalse(String organizationName);
//
//    /**
//     * Search supervisors by name
//     */
    @Query("SELECT s FROM MedicalSupervisor s WHERE s.isDeleted = false AND " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<MedicalSupervisor> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Count all active supervisors
     */
//    @Query("SELECT COUNT(s) FROM MedicalSupervisor s WHERE s.isDeleted = false AND s.isAvailable = true")
//    long countActiveSupervisors();

    /**
     * Find all assignments for a supervisor by status
     */
    List<SupervisorPatientAssignment> findBySupervisorIdAndAssignmentStatus(
            Long supervisorId,
            SupervisorAssignmentStatus status);


    /**
     * Find active assignment by supervisor and patient
     */
    Optional<SupervisorPatientAssignment> findBySupervisorIdAndPatientIdAndAssignmentStatus(
            Long supervisorId,
            Long patientId,
            SupervisorAssignmentStatus status);

    /**
     * Check if patient is assigned to supervisor
     */
    boolean existsBySupervisorIdAndPatientIdAndAssignmentStatus(
            Long supervisorId,
            Long patientId,
            SupervisorAssignmentStatus status);

    /**
     * Count assignments for a supervisor by status
     */
    long countBySupervisorIdAndAssignmentStatus(
            Long supervisorId,
            SupervisorAssignmentStatus status);

    /**
     * Find all active assignments for a supervisor
     */
    @Query("SELECT a FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
            "AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    List<SupervisorPatientAssignment> findActiveAssignments(@Param("supervisorId") Long supervisorId);

    /**
     * Get patient IDs assigned to supervisor
     */
    @Query("SELECT a.patientId FROM SupervisorPatientAssignment a WHERE a.supervisor.id = :supervisorId " +
            "AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    List<Long> findPatientIdsBySupervisorId(@Param("supervisorId") Long supervisorId);

    /**
     * Find assignments by patient ID
     */
    List<SupervisorPatientAssignment> findByPatientIdAndIsDeletedFalse(Long patientId);

    /**
     * Check if patient is managed by any supervisor
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM SupervisorPatientAssignment a " +
            "WHERE a.patientId = :patientId AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    boolean isPatientManagedBySupervisor(@Param("patientId") Long patientId);

    /**
     * Find supervisor ID for a patient
     */
    @Query("SELECT a.supervisor.id FROM SupervisorPatientAssignment a " +
            "WHERE a.patientId = :patientId AND a.assignmentStatus = 'ACTIVE' AND a.isDeleted = false")
    Optional<Long> findSupervisorIdByPatientId(@Param("patientId") Long patientId);
}