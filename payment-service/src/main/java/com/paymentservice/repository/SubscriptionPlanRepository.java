package com.paymentservice.repository;

import com.paymentservice.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    
    Optional<SubscriptionPlan> findByStripePriceId(String stripePriceId);
    
    Optional<SubscriptionPlan> findByStripeProductId(String stripeProductId);
    
    List<SubscriptionPlan> findByUserTypeAndIsActive(String userType, boolean isActive);
    
    List<SubscriptionPlan> findByPlanTypeAndUserTypeAndIsActive(String planType, String userType, boolean isActive);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.userType = :userType AND sp.planType = :planType AND sp.durationMonths = :duration AND sp.isActive = true")
    Optional<SubscriptionPlan> findActivePlan(@Param("userType") String userType, 
                                             @Param("planType") String planType, 
                                             @Param("duration") Integer duration);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true ORDER BY sp.userType, sp.planType, sp.durationMonths")
    List<SubscriptionPlan> findAllActivePlansOrdered();
    
    @Query("SELECT DISTINCT sp.planType FROM SubscriptionPlan sp WHERE sp.userType = :userType AND sp.isActive = true")
    List<String> findAvailablePlanTypes(@Param("userType") String userType);
}