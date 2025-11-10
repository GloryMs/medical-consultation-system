package com.paymentservice.repository;

import com.paymentservice.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findByUserIdAndUserType(Long userId, String userType);
    
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
    
    List<Subscription> findByStatus(String status);
    
    List<Subscription> findByUserType(String userType);
    
    List<Subscription> findByPlanType(String planType);
    
    // Active subscriptions
    @Query("SELECT s FROM Subscription s WHERE s.status IN ('active', 'trialing')")
    List<Subscription> findActiveSubscriptions();
    
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.userType = :userType AND s.status IN ('active', 'trialing')")
    Optional<Subscription> findActiveByUserIdAndUserType(@Param("userId") Long userId, @Param("userType") String userType);
    
    // Trial ending soon
    @Query("SELECT s FROM Subscription s WHERE s.status = 'trialing' AND s.trialEnd BETWEEN :start AND :end")
    List<Subscription> findTrialEndingSoon(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Subscriptions expiring soon
    @Query("SELECT s FROM Subscription s WHERE s.status = 'active' AND s.currentPeriodEnd BETWEEN :start AND :end")
    List<Subscription> findExpiringSubscriptions(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Canceled subscriptions
    @Query("SELECT s FROM Subscription s WHERE s.status = 'canceled' AND s.canceledAt BETWEEN :start AND :end")
    List<Subscription> findCanceledBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Count queries
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.userType = :userType AND s.status IN ('active', 'trialing')")
    Long countActiveByUserType(@Param("userType") String userType);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.planType = :planType AND s.status IN ('active', 'trialing')")
    Long countActiveByPlanType(@Param("planType") String planType);
    
    // Pagination
    Page<Subscription> findByUserType(String userType, Pageable pageable);
    
    Page<Subscription> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT s FROM Subscription s WHERE s.status IN ('active', 'trialing') ORDER BY s.createdAt DESC")
    Page<Subscription> findActiveSubscriptionsPaged(Pageable pageable);
}