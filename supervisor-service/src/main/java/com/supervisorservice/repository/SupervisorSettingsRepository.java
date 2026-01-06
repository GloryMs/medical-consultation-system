package com.supervisorservice.repository;

import com.supervisorservice.entity.SupervisorSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SupervisorSettings entity
 */
@Repository
public interface SupervisorSettingsRepository extends JpaRepository<SupervisorSettings, Long> {
    
    /**
     * Find settings by supervisor ID
     */
    Optional<SupervisorSettings> findBySupervisorId(Long supervisorId);
    
    /**
     * Find settings by supervisor user ID
     */
    @Query("SELECT s FROM SupervisorSettings s WHERE s.supervisor.userId = :userId")
    Optional<SupervisorSettings> findBySupervisorUserId(@Param("userId") Long userId);
    
    /**
     * Check if settings exist for supervisor
     */
    boolean existsBySupervisorId(Long supervisorId);
    
    /**
     * Delete settings by supervisor ID
     */
    void deleteBySupervisorId(Long supervisorId);
}