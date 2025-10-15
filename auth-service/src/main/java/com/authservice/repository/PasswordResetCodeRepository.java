package com.authservice.repository;

import com.authservice.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PasswordResetCode entity
 * Provides database operations for password reset code management
 */
@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    
    /**
     * Find a valid reset code by identifier and code that is not used and not expired
     * This is the main method used during password reset verification
     * 
     * @param identifier User's email or phone number
     * @param code 6-digit verification code
     * @param currentTime Current timestamp to check expiry
     * @return Optional containing the PasswordResetCode if found and valid
     */
    Optional<PasswordResetCode> findByIdentifierAndCodeAndIsUsedFalseAndExpiryTimeAfter(
        String identifier, 
        String code, 
        LocalDateTime currentTime
    );
    
    /**
     * Find the most recent unused reset code for an identifier
     * Used to check if a user already has a pending code
     * 
     * @param identifier User's email or phone number
     * @return Optional containing the most recent unused code
     */
    Optional<PasswordResetCode> findFirstByIdentifierAndIsUsedFalseOrderByCreatedAtDesc(
        String identifier
    );
    
    /**
     * Find all reset codes for a specific identifier (for audit purposes)
     * 
     * @param identifier User's email or phone number
     * @return List of all codes for this identifier
     */
    List<PasswordResetCode> findByIdentifierOrderByCreatedAtDesc(String identifier);
    
    /**
     * Delete all expired codes (used in scheduled cleanup task)
     * 
     * @param expiryTime Timestamp - codes expiring before this will be deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetCode p WHERE p.expiryTime < :expiryTime")
    void deleteByExpiryTimeBefore(@Param("expiryTime") LocalDateTime expiryTime);
    
    /**
     * Delete all codes for a specific identifier
     * Used when generating a new code to clean up old ones
     * 
     * @param identifier User's email or phone number
     */
    @Modifying
    @Transactional
    void deleteByIdentifier(String identifier);
    
    /**
     * Count unused codes for an identifier
     * Used to prevent abuse (limit number of pending codes)
     * 
     * @param identifier User's email or phone number
     * @return Count of unused codes
     */
    @Query("SELECT COUNT(p) FROM PasswordResetCode p WHERE p.identifier = :identifier AND p.isUsed = false")
    long countUnusedCodesByIdentifier(@Param("identifier") String identifier);
    
    /**
     * Check if a valid (unused and not expired) code exists for identifier
     * 
     * @param identifier User's email or phone number
     * @param currentTime Current timestamp
     * @return true if valid code exists
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PasswordResetCode p " +
           "WHERE p.identifier = :identifier AND p.isUsed = false AND p.expiryTime > :currentTime")
    boolean existsValidCodeForIdentifier(@Param("identifier") String identifier, 
                                         @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all codes that will expire soon
     * Used for monitoring and alerting purposes
     * 
     * @param now Current timestamp
     * @param threshold Future timestamp (e.g., 2 minutes from now)
     * @return List of codes expiring between now and threshold
     */
    @Query("SELECT p FROM PasswordResetCode p WHERE p.isUsed = false " +
           "AND p.expiryTime > :now AND p.expiryTime < :threshold")
    List<PasswordResetCode> findCodesExpiringSoon(@Param("now") LocalDateTime now, 
                                                   @Param("threshold") LocalDateTime threshold);
    
    /**
     * Get count of codes generated since a specific time
     * Used for statistics and reporting
     * 
     * @param since Starting timestamp
     * @return Count of codes created since this time
     */
    @Query("SELECT COUNT(p) FROM PasswordResetCode p WHERE p.createdAt >= :since")
    long countCodesSince(@Param("since") LocalDateTime since);
    
    /**
     * Count successful password resets (used codes) since a specific time
     * Used for statistics and reporting
     * 
     * @param since Starting timestamp
     * @return Count of successfully used codes
     */
    @Query("SELECT COUNT(p) FROM PasswordResetCode p WHERE p.isUsed = true AND p.updatedAt >= :since")
    long countSuccessfulResetsSince(@Param("since") LocalDateTime since);
    
    /**
     * Find codes that have reached maximum attempts
     * Used in cleanup task to mark them as invalid
     * 
     * @param maxAttempts Maximum allowed attempts (usually 3)
     * @return List of codes with attempts >= maxAttempts
     */
    @Query("SELECT p FROM PasswordResetCode p WHERE p.attemptCount >= :maxAttempts AND p.isUsed = false")
    List<PasswordResetCode> findCodesWithMaxAttempts(@Param("maxAttempts") int maxAttempts);
    
    /**
     * Count codes by delivery method (for analytics)
     * 
     * @param deliveryMethod EMAIL, SMS, or WHATSAPP
     * @param since Starting timestamp
     * @return Count of codes sent via this method
     */
    @Query("SELECT COUNT(p) FROM PasswordResetCode p WHERE p.deliveryMethod = :deliveryMethod AND p.createdAt >= :since")
    long countByDeliveryMethodSince(@Param("deliveryMethod") String deliveryMethod, 
                                    @Param("since") LocalDateTime since);
    
    /**
     * Find codes by identifier and delivery method
     * 
     * @param identifier User's email or phone number
     * @param deliveryMethod EMAIL, SMS, or WHATSAPP
     * @return List of codes matching criteria
     */
    List<PasswordResetCode> findByIdentifierAndDeliveryMethodOrderByCreatedAtDesc(
        String identifier, 
        String deliveryMethod
    );
}