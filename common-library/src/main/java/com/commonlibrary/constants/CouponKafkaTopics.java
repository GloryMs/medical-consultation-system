package com.commonlibrary.constants;

/**
 * Constants for coupon-related Kafka topics.
 * Used across admin-service, supervisor-service, patient-service, and payment-service.
 */
public final class CouponKafkaTopics {
    
    private CouponKafkaTopics() {
        // Prevent instantiation
    }
    
    // ==================== Admin-Service Published Topics ====================
    
    /**
     * Published when a coupon is created
     */
    public static final String COUPON_CREATED = "admin.coupon.created";
    
    /**
     * Published when a coupon is distributed to a beneficiary
     */
    public static final String COUPON_DISTRIBUTED = "admin.coupon.distributed";
    
    /**
     * Published when a coupon is marked as used
     */
    public static final String COUPON_USED = "admin.coupon.used";
    
    /**
     * Published when a coupon is cancelled
     */
    public static final String COUPON_CANCELLED = "admin.coupon.cancelled";
    
    /**
     * Published when coupons expire (scheduled job)
     */
    public static final String COUPON_EXPIRED = "admin.coupon.expired";
    
    /**
     * Published when a coupon batch is created
     */
    public static final String COUPON_BATCH_CREATED = "admin.coupon.batch.created";
    
    /**
     * Published when a coupon batch is distributed
     */
    public static final String COUPON_BATCH_DISTRIBUTED = "admin.coupon.batch.distributed";
    
    // ==================== Supervisor-Service Published Topics ====================
    
    /**
     * Published when supervisor assigns a coupon to a patient
     */
    public static final String SUPERVISOR_COUPON_ASSIGNED = "supervisor.coupon.assigned";
    
    /**
     * Published when supervisor unassigns a coupon from a patient
     */
    public static final String SUPERVISOR_COUPON_UNASSIGNED = "supervisor.coupon.unassigned";
    
    // ==================== Payment-Service Published Topics ====================
    
    /**
     * Published when a coupon payment is completed
     */
    public static final String COUPON_PAYMENT_COMPLETED = "payment.coupon.completed";
    
    /**
     * Published when a coupon payment fails
     */
    public static final String COUPON_PAYMENT_FAILED = "payment.coupon.failed";
    
    // ==================== Notification Topics ====================
    
    /**
     * Published for coupon-related notifications
     */
    public static final String COUPON_NOTIFICATION = "coupon.notification";
    
    /**
     * Published when coupons are expiring soon (warning)
     */
    public static final String COUPON_EXPIRING_SOON = "coupon.expiring.soon";
    
    // ==================== Consumer Group IDs ====================
    
    public static final String SUPERVISOR_SERVICE_GROUP = "supervisor-service-coupon-group";
    public static final String PATIENT_SERVICE_GROUP = "patient-service-coupon-group";
    public static final String PAYMENT_SERVICE_GROUP = "payment-service-coupon-group";
    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service-coupon-group";
    public static final String ADMIN_SERVICE_GROUP = "admin-service-coupon-group";
}