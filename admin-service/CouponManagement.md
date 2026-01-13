Coupon Management from Admin with coordination (supervisor, payment ).

No migration needed - Tables are empty, we can drop/recreate
Coupon validation/redemption - Must be coordinated across admin-service, supervisor-service, and payment-service


Revised Refactoring Plan
Updated Architecture Flow
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    COUPON LIFECYCLE & VALIDATION FLOW                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                      ADMIN-SERVICE (Master Owner)                        │    │
│  │                                                                          │    │
│  │  Responsibilities:                                                       │    │
│  │  ✓ Create coupons (single/batch)                                        │    │
│  │  ✓ Define discount type & value                                         │    │
│  │  ✓ Distribute to Supervisors or Patients                                │    │
│  │  ✓ Track global coupon status                                           │    │
│  │  ✓ Cancel/expire coupons                                                │    │
│  │  ✓ Validate coupon existence & global status                            │    │
│  │  ✓ Mark coupon as USED (final authority)                                │    │
│  │  ✓ Analytics & reporting                                                │    │
│  │                                                                          │    │
│  │  Validation Endpoint: POST /api/admin/coupons/validate                  │    │
│  │  Mark Used Endpoint:  POST /api/admin/coupons/{code}/mark-used          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                              │                                                   │
│              ┌───────────────┴───────────────┐                                  │
│              │ Kafka: coupon.distributed      │                                  │
│              │ Kafka: coupon.cancelled        │                                  │
│              ▼                                ▼                                  │
│  ┌──────────────────────────┐    ┌──────────────────────────┐                   │
│  │   SUPERVISOR-SERVICE     │    │    PATIENT-SERVICE       │                   │
│  │                          │    │    (Direct Coupons)      │                   │
│  │  Responsibilities:       │    │                          │                   │
│  │  ✓ Receive allocated     │    │  Responsibilities:       │                   │
│  │    coupons               │    │  ✓ Receive direct        │                   │
│  │  ✓ Assign to patients    │    │    patient coupons       │                   │
│  │  ✓ Local validation      │    │  ✓ Display to patient    │                   │
│  │    (ownership check)     │    │                          │                   │
│  │  ✓ Initiate redemption   │    │                          │                   │
│  └────────────┬─────────────┘    └──────────────────────────┘                   │
│               │                                                                  │
│               │ Supervisor initiates payment                                     │
│               ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                      PAYMENT-SERVICE (Transaction Owner)                 │    │
│  │                                                                          │    │
│  │  Responsibilities:                                                       │    │
│  │  ✓ Receive payment request (COUPON method)                              │    │
│  │  ✓ Call Admin-Service to validate coupon                                │    │
│  │  ✓ Calculate discount & final amount                                    │    │
│  │  ✓ Process payment (if partial, charge remaining via Stripe)            │    │
│  │  ✓ Call Admin-Service to mark coupon as USED                            │    │
│  │  ✓ Create payment record                                                │    │
│  │  ✓ Publish payment.completed event                                      │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│               │                                                                  │
│               │ Kafka: payment.completed                                         │
│               ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │  ADMIN-SERVICE & SUPERVISOR-SERVICE (Consumers)                          │    │
│  │  ✓ Update local coupon status to USED                                   │    │
│  │  ✓ Record redemption details                                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

Validation & Redemption Flow (Detailed)
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         COUPON REDEMPTION SEQUENCE                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  STEP 1: Supervisor Initiates Payment                                           │
│  ─────────────────────────────────────                                          │
│  Supervisor-Service: POST /api/supervisors/payments/pay                         │
│  {                                                                               │
│    "caseId": 1001,                                                              │
│    "patientId": 50,                                                             │
│    "paymentMethod": "COUPON",                                                   │
│    "couponCode": "MED-2026-ABC123"                                              │
│  }                                                                               │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 2: Supervisor-Service Local Validation                                    │
│  ───────────────────────────────────────────                                    │
│  ✓ Check coupon exists in supervisor_coupon_allocations                        │
│  ✓ Check supervisor owns this coupon                                           │
│  ✓ Check coupon is assigned to this patient                                    │
│  ✓ Check local status = AVAILABLE or ASSIGNED                                  │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 3: Call Payment-Service                                                   │
│  ────────────────────────────                                                   │
│  Supervisor-Service → Payment-Service (Feign Client)                            │
│  POST /api/payments/process-coupon-payment                                      │
│  {                                                                               │
│    "caseId": 1001,                                                              │
│    "patientId": 50,                                                             │
│    "supervisorId": 100,                                                         │
│    "couponCode": "MED-2026-ABC123",                                             │
│    "consultationFee": 150.00                                                    │
│  }                                                                               │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 4: Payment-Service Validates with Admin-Service                           │
│  ─────────────────────────────────────────────────────                          │
│  Payment-Service → Admin-Service (Feign Client)                                 │
│  POST /api/admin/coupons/validate                                               │
│  {                                                                               │
│    "couponCode": "MED-2026-ABC123",                                             │
│    "beneficiaryType": "MEDICAL_SUPERVISOR",                                     │
│    "beneficiaryId": 100,                                                        │
│    "patientId": 50,                                                             │
│    "caseId": 1001,                                                              │
│    "requestedAmount": 150.00                                                    │
│  }                                                                               │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 5: Admin-Service Validates (Source of Truth)                              │
│  ─────────────────────────────────────────────────                              │
│  ✓ Coupon exists in admin_coupons                                              │
│  ✓ Status = DISTRIBUTED (not USED, EXPIRED, CANCELLED)                         │
│  ✓ Beneficiary matches                                                          │
│  ✓ Not expired (expires_at > now)                                              │
│  ✓ Calculate discount amount                                                    │
│                              │                                                   │
│  Returns:                    │                                                   │
│  {                           │                                                   │
│    "valid": true,            │                                                   │
│    "couponId": 12345,        │                                                   │
│    "discountType": "FULL_COVERAGE",                                             │
│    "discountValue": 150.00,  │                                                   │
│    "discountAmount": 150.00, │                                                   │
│    "remainingAmount": 0.00   │                                                   │
│  }                           │                                                   │
│                              ▼                                                   │
│  STEP 6: Payment-Service Processes Payment                                      │
│  ─────────────────────────────────────────                                      │
│  IF remainingAmount > 0:                                                        │
│     → Charge via Stripe                                                         │
│  IF remainingAmount = 0:                                                        │
│     → No charge needed (full coverage)                                          │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 7: Payment-Service Marks Coupon as Used                                   │
│  ────────────────────────────────────────────                                   │
│  Payment-Service → Admin-Service (Feign Client)                                 │
│  POST /api/admin/coupons/{couponCode}/mark-used                                 │
│  {                                                                               │
│    "caseId": 1001,                                                              │
│    "patientId": 50,                                                             │
│    "paymentId": 5001,                                                           │
│    "usedAt": "2026-01-11T10:30:00"                                              │
│  }                                                                               │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 8: Admin-Service Updates Master Record                                    │
│  ───────────────────────────────────────────                                    │
│  UPDATE admin_coupons SET status = 'USED', used_at = NOW(),                     │
│         used_for_case_id = 1001, used_for_payment_id = 5001                     │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 9: Payment-Service Creates Payment Record                                 │
│  ──────────────────────────────────────────────                                 │
│  INSERT INTO payments (case_id, patient_id, amount, coupon_id, ...)            │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 10: Kafka Events Published                                                │
│  ───────────────────────────────                                                │
│  Payment-Service → payment.completed                                            │
│  Admin-Service → coupon.used                                                    │
│                              │                                                   │
│                              ▼                                                   │
│  STEP 11: Supervisor-Service Updates Local Record                               │
│  ────────────────────────────────────────────────                               │
│  Consumes: payment.completed OR coupon.used                                     │
│  UPDATE supervisor_coupon_allocations SET status = 'USED', ...                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

Revised Database Schema
Admin-Service Database
sql-- Master coupon table (Source of Truth)
CREATE TABLE admin_coupons (
id BIGSERIAL PRIMARY KEY,
coupon_code VARCHAR(50) NOT NULL UNIQUE,

    -- Discount configuration
    discount_type VARCHAR(20) NOT NULL,       -- PERCENTAGE, FIXED_AMOUNT, FULL_COVERAGE
    discount_value DECIMAL(10,2) NOT NULL,    -- Percentage or fixed amount
    max_discount_amount DECIMAL(10,2),        -- Cap for percentage discounts
    currency VARCHAR(10) DEFAULT 'USD',
    
    -- Beneficiary info
    beneficiary_type VARCHAR(20) NOT NULL,    -- MEDICAL_SUPERVISOR, PATIENT
    beneficiary_id BIGINT,                    -- NULL = unassigned pool
    
    -- Status tracking
    status VARCHAR(20) DEFAULT 'CREATED',     -- CREATED, DISTRIBUTED, USED, EXPIRED, CANCELLED
    
    -- Batch reference
    batch_id BIGINT,
    
    -- Admin tracking
    created_by BIGINT NOT NULL,               -- Admin user ID who created
    distributed_by BIGINT,                    -- Admin user ID who distributed
    distributed_at TIMESTAMP,
    
    -- Usage tracking (updated when redeemed)
    used_at TIMESTAMP,
    used_for_case_id BIGINT,
    used_for_payment_id BIGINT,
    used_by_patient_id BIGINT,
    
    -- Expiration
    expires_at TIMESTAMP NOT NULL,
    
    -- Metadata
    notes TEXT,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    cancelled_by BIGINT,
    
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Batch management
CREATE TABLE admin_coupon_batches (
id BIGSERIAL PRIMARY KEY,
batch_code VARCHAR(50) NOT NULL UNIQUE,

    -- Batch configuration
    beneficiary_type VARCHAR(20) NOT NULL,    -- MEDICAL_SUPERVISOR, PATIENT
    beneficiary_id BIGINT,                    -- Target (NULL = pool for later distribution)
    total_coupons INT NOT NULL,
    
    -- Discount config for all coupons in batch
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'USD',
    
    -- Expiration
    expiry_days INT DEFAULT 180,              -- Days from creation/distribution
    
    -- Tracking
    created_by BIGINT NOT NULL,
    distributed_by BIGINT,
    distributed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'CREATED',     -- CREATED, DISTRIBUTED, PARTIALLY_USED, FULLY_USED, CANCELLED
    
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Coupon usage/redemption history (audit trail)
CREATE TABLE coupon_redemption_history (
id BIGSERIAL PRIMARY KEY,
coupon_id BIGINT NOT NULL REFERENCES admin_coupons(id),
coupon_code VARCHAR(50) NOT NULL,

    -- Who redeemed
    redeemed_by_type VARCHAR(20) NOT NULL,    -- MEDICAL_SUPERVISOR, PATIENT
    redeemed_by_id BIGINT NOT NULL,
    
    -- For whom
    patient_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    
    -- Amounts
    original_amount DECIMAL(10,2) NOT NULL,   -- Consultation fee
    discount_applied DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2) NOT NULL,      -- Amount charged (if any)
    
    redeemed_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_admin_coupons_code ON admin_coupons(coupon_code);
CREATE INDEX idx_admin_coupons_status ON admin_coupons(status);
CREATE INDEX idx_admin_coupons_beneficiary ON admin_coupons(beneficiary_type, beneficiary_id);
CREATE INDEX idx_admin_coupons_batch ON admin_coupons(batch_id);
CREATE INDEX idx_admin_coupons_expires ON admin_coupons(expires_at);
Supervisor-Service Database (Refactored)
sql-- DROP old tables (since they're empty)
DROP TABLE IF EXISTS supervisor_coupons;
DROP TABLE IF EXISTS coupon_batches;

-- New allocation table (local copy for fast access)
CREATE TABLE supervisor_coupon_allocations (
id BIGSERIAL PRIMARY KEY,

    -- Reference to master record
    admin_coupon_id BIGINT NOT NULL,          -- FK to admin_coupons (logical, not physical)
    coupon_code VARCHAR(50) NOT NULL,
    
    -- Ownership
    supervisor_id BIGINT NOT NULL,
    
    -- Assignment to patient (supervisor assigns)
    assigned_patient_id BIGINT,               -- NULL = not yet assigned to patient
    assigned_at TIMESTAMP,
    
    -- Discount info (copied from admin for fast access)
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'USD',
    
    -- Local status
    status VARCHAR(20) DEFAULT 'AVAILABLE',   -- AVAILABLE, ASSIGNED, USED, EXPIRED, CANCELLED
    
    -- Usage tracking
    used_at TIMESTAMP,
    used_for_case_id BIGINT,
    used_for_payment_id BIGINT,
    
    -- Expiration (copied from admin)
    expires_at TIMESTAMP NOT NULL,
    
    -- Sync tracking
    received_at TIMESTAMP DEFAULT NOW(),      -- When received from admin
    last_synced_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT unique_coupon_allocation UNIQUE(coupon_code, supervisor_id)
);

-- Indexes
CREATE INDEX idx_sup_coupon_alloc_supervisor ON supervisor_coupon_allocations(supervisor_id);
CREATE INDEX idx_sup_coupon_alloc_patient ON supervisor_coupon_allocations(assigned_patient_id);
CREATE INDEX idx_sup_coupon_alloc_status ON supervisor_coupon_allocations(status);
CREATE INDEX idx_sup_coupon_alloc_code ON supervisor_coupon_allocations(coupon_code);
Payment-Service Database (Updates)
sql-- Add columns to existing payments table (if not exists)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS coupon_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_source VARCHAR(20) DEFAULT 'DIRECT';  -- DIRECT, COUPON, MIXED

-- Coupon redemptions table (for detailed tracking)
CREATE TABLE IF NOT EXISTS coupon_redemptions (
id BIGSERIAL PRIMARY KEY,
payment_id BIGINT NOT NULL REFERENCES payments(id),
coupon_id BIGINT NOT NULL,                -- Reference to admin_coupons
coupon_code VARCHAR(50) NOT NULL,

    case_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    
    -- Who initiated
    redeemed_by_type VARCHAR(20) NOT NULL,    -- MEDICAL_SUPERVISOR, PATIENT
    redeemed_by_id BIGINT NOT NULL,
    
    -- Amounts
    original_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    charged_amount DECIMAL(10,2) NOT NULL,    -- Via Stripe (0 if full coverage)
    
    redeemed_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

Service Responsibilities Summary
ServiceCreateDistributeValidateMark UsedLocal StatusAdmin✅✅✅ (Master)✅ (Master)✅Supervisor❌❌✅ (Local ownership)❌✅ (via Kafka)Payment❌❌✅ (Calls Admin)✅ (Calls Admin)✅Patient❌❌❌❌✅ (via Kafka)

New Enums (common-library)
java// BeneficiaryType.java
public enum BeneficiaryType {
MEDICAL_SUPERVISOR,
PATIENT
}

// CouponStatus.java (UPDATED)
public enum CouponStatus {
CREATED,        // Created but not distributed
DISTRIBUTED,    // Distributed to beneficiary
ASSIGNED,       // Supervisor assigned to patient (supervisor-service only)
AVAILABLE,      // Ready for use (supervisor-service local)
USED,           // Redeemed for payment
EXPIRED,        // Past expiration date
CANCELLED       // Manually cancelled
}

API Endpoints Summary
Admin-Service (New)
EndpointMethodDescription/api/admin/couponsPOSTCreate single coupon/api/admin/coupons/batchPOSTCreate batch coupons/api/admin/couponsGETList coupons (with filters)/api/admin/coupons/{id}GETGet coupon details/api/admin/coupons/{id}/distributePOSTDistribute single coupon/api/admin/coupons/batch/{batchId}/distributePOSTDistribute entire batch/api/admin/coupons/{id}/cancelPOSTCancel coupon/api/admin/coupons/validatePOSTValidate coupon (called by payment-service)/api/admin/coupons/{code}/mark-usedPOSTMark as used (called by payment-service)/api/admin/coupons/beneficiary/{type}/{id}GETGet coupons for beneficiary/api/admin/coupons/analyticsGETAnalytics & reports/api/admin/coupons/expiringGETGet soon-to-expire coupons
Supervisor-Service (Refactored)
EndpointMethodChangeDescription/api/supervisors/couponsGETKEEPList allocated coupons/api/supervisors/coupons/availableGETNEWList unassigned coupons/api/supervisors/coupons/{id}/assignPOSTNEWAssign coupon to patient/api/supervisors/coupons/{id}/unassignPOSTNEWUnassign from patient/api/supervisors/coupons/patient/{patientId}GETKEEPGet patient's coupons/api/supervisors/payments/validate-couponPOSTREFACTORValidate (local + call admin)/api/supervisors/payments/payPOSTKEEPProcess payment (calls payment-service)
REMOVED from Supervisor:

Coupon creation endpoints
Batch creation endpoints
Direct coupon issuance

Payment-Service (Updates)
EndpointMethodChangeDescription/api/payments/process-coupon-paymentPOSTNEWProcess coupon payment (internal)/api/payments/coupon-redemptionsGETNEWGet redemption history

Kafka Topics
TopicProducerConsumersPayloadadmin.coupon.createdadmin-servicenotification-serviceCoupon detailsadmin.coupon.distributedadmin-servicesupervisor-service, patient-serviceCoupon + beneficiaryadmin.coupon.cancelledadmin-servicesupervisor-service, patient-serviceCoupon code + reasonadmin.coupon.usedadmin-servicesupervisor-service, notification-serviceRedemption detailsadmin.coupon.expiredadmin-service (scheduled)supervisor-service, patient-serviceCoupon codespayment.completedpayment-serviceadmin-service, supervisor-service, patient-servicePayment details

Implementation Phases
PhaseTasksServices Affected1Create new enums in common-librarycommon-library2Create admin-service coupon tables, entities, repositoriesadmin-service3Implement admin coupon service & controlleradmin-service4Implement admin Kafka produceradmin-service5Refactor supervisor-service tables (drop old, create new)supervisor-service6Implement supervisor Kafka consumersupervisor-service7Refactor supervisor coupon service (remove creation)supervisor-service8Add Feign client in payment-service to call admin-servicepayment-service9Implement payment-service coupon validation & redemptionpayment-service10Implement admin-service internal validation endpointsadmin-service11Update supervisor payment flow to use payment-servicesupervisor-service12Frontend: Admin coupon management pagesFrontend13Frontend: Refactor supervisor coupon pagesFrontend14Testing & integrationAll

Key Points

Admin-Service is Source of Truth - All coupon existence and status checks ultimately go through admin-service
Payment-Service Orchestrates Redemption - Payment-service calls admin to validate, processes payment, then calls admin to mark used
Supervisor-Service Has Local Copy - For fast reads and assignment to patients, synced via Kafka
No Direct DB Access Across Services - All cross-service communication via Feign clients or Kafka
Atomic Operations - Coupon marking as USED happens in admin-service within transaction


Shall I proceed with detailed implementation starting from Phase 1 (common-library enums)?