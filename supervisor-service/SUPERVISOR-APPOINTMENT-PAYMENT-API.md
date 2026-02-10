# Supervisor Appointment & Payment API Documentation

**Version:** 1.0
**Base URL:** `/api/supervisors`
**Authentication:** Required (X-User-Id header)

This documentation covers the API endpoints for managing appointments and payments on behalf of assigned patients in the Medical Supervisor Service.

---

## Table of Contents

1. [Authentication](#authentication)
2. [Appointment Management APIs](#appointment-management-apis)
3. [Payment Management APIs](#payment-management-apis)
4. [Data Models](#data-models)
5. [Enumerations](#enumerations)
6. [Error Handling](#error-handling)

---

## Authentication

All endpoints require a valid supervisor user ID in the request header:

```http
X-User-Id: <supervisor-user-id>
```

---

## Appointment Management APIs

**Base Path:** `/api/supervisors/appointments`

### 1. Get All Appointments

Retrieves appointments for all patients assigned to the supervisor with advanced filtering options.

**Endpoint:** `GET /api/supervisors/appointments`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patientId | Long | No | Filter by patient ID |
| caseId | Long | No | Filter by case ID |
| status | AppointmentStatus | No | Filter by status (SCHEDULED, CONFIRMED, COMPLETED, etc.) |
| date | LocalDate | No | Filter by specific date (ISO format: YYYY-MM-DD) |
| startDate | LocalDate | No | Start date for date range filter |
| endDate | LocalDate | No | End date for date range filter |
| upcomingOnly | Boolean | No | Show only upcoming appointments (default: false) |
| sortBy | String | No | Sort field (default: "scheduledTime") |
| sortOrder | String | No | Sort order: "asc" or "desc" (default: "asc") |

**Request Example:**

```http
GET /api/supervisors/appointments?patientId=101&status=SCHEDULED&upcomingOnly=true&sortBy=scheduledTime&sortOrder=asc
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 2001,
      "caseId": 3001,
      "caseTitle": "Chronic Migraine Consultation",
      "doctor": {
        "id": 4001,
        "fullName": "Dr. Sarah Johnson",
        "email": "sarah.johnson@hospital.com",
        "primarySpecialization": "Neurology"
      },
      "doctorId": 4001,
      "doctorName": "Dr. Sarah Johnson",
      "patientId": 101,
      "patientName": "John Doe",
      "patientEmail": "john.doe@email.com",
      "consultationFee": 150.00,
      "scheduledTime": "2026-01-15T10:00:00",
      "duration": 30,
      "consultationType": "ZOOM",
      "status": "SCHEDULED",
      "rescheduleCount": 0,
      "meetingLink": "https://zoom.us/j/123456789",
      "meetingId": "123456789",
      "rescheduledFrom": null,
      "rescheduleReason": null,
      "completedAt": null,
      "supervisorId": 5001,
      "isSupervisorManaged": true,
      "hasPendingRescheduleRequest": false,
      "rescheduleRequestId": null,
      "specialization": "Neurology",
      "createdAt": "2026-01-08T09:00:00",
      "updatedAt": "2026-01-08T09:00:00"
    }
  ],
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 2. Get Upcoming Appointments

Retrieves all upcoming appointments for assigned patients.

**Endpoint:** `GET /api/supervisors/appointments/upcoming`

**Request Example:**

```http
GET /api/supervisors/appointments/upcoming
Headers:
  X-User-Id: 5001
```

**Response:** Same structure as "Get All Appointments"

---

### 3. Get Appointment Details

Retrieves detailed information about a specific appointment.

**Endpoint:** `GET /api/supervisors/appointments/{appointmentId}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| appointmentId | Long | Yes | Appointment ID |

**Request Example:**

```http
GET /api/supervisors/appointments/2001
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 2001,
    "caseId": 3001,
    "caseTitle": "Chronic Migraine Consultation",
    "doctor": {
      "id": 4001,
      "fullName": "Dr. Sarah Johnson",
      "email": "sarah.johnson@hospital.com",
      "primarySpecialization": "Neurology"
    },
    "doctorId": 4001,
    "doctorName": "Dr. Sarah Johnson",
    "patientId": 101,
    "patientName": "John Doe",
    "patientEmail": "john.doe@email.com",
    "consultationFee": 150.00,
    "scheduledTime": "2026-01-15T10:00:00",
    "duration": 30,
    "consultationType": "ZOOM",
    "status": "SCHEDULED",
    "rescheduleCount": 0,
    "meetingLink": "https://zoom.us/j/123456789",
    "meetingId": "123456789",
    "supervisorId": 5001,
    "isSupervisorManaged": true,
    "hasPendingRescheduleRequest": false,
    "specialization": "Neurology",
    "createdAt": "2026-01-08T09:00:00",
    "updatedAt": "2026-01-08T09:00:00"
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 4. Get Patient Appointments

Retrieves all appointments for a specific assigned patient.

**Endpoint:** `GET /api/supervisors/appointments/patient/{patientId}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patientId | Long | Yes | Patient ID |

**Request Example:**

```http
GET /api/supervisors/appointments/patient/101
Headers:
  X-User-Id: 5001
```

**Response:** Array of appointments (same structure as "Get All Appointments")

---

### 5. Get Case Appointments

Retrieves all appointments associated with a specific case.

**Endpoint:** `GET /api/supervisors/appointments/case/{caseId}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| caseId | Long | Yes | Case ID |

**Request Example:**

```http
GET /api/supervisors/appointments/case/3001
Headers:
  X-User-Id: 5001
```

**Response:** Array of appointments (same structure as "Get All Appointments")

---

### 6. Accept Appointment

Accepts a scheduled appointment on behalf of the patient. This moves the case to payment pending status.

**Endpoint:** `POST /api/supervisors/appointments/accept`

**Request Body:**

```json
{
  "caseId": 3001,
  "patientId": 101,
  "notes": "Patient confirmed availability for this time slot"
}
```

**Request Example:**

```http
POST /api/supervisors/appointments/accept
Headers:
  X-User-Id: 5001
  Content-Type: application/json

Body:
{
  "caseId": 3001,
  "patientId": 101,
  "notes": "Patient confirmed availability for this time slot"
}
```

**Response Example:**

```json
{
  "success": true,
  "message": "Appointment accepted successfully",
  "data": null,
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 7. Create Reschedule Request

Creates a reschedule request for an appointment on behalf of the patient.

**Endpoint:** `POST /api/supervisors/appointments/reschedule-request`

**Request Body:**

```json
{
  "appointmentId": 2001,
  "caseId": 3001,
  "patientId": 101,
  "preferredTimes": [
    "2026-01-16T10:00:00",
    "2026-01-16T14:00:00",
    "2026-01-17T09:00:00"
  ],
  "reason": "Patient has a conflict with the original time",
  "additionalNotes": "Patient prefers morning slots"
}
```

**Validation Rules:**
- `preferredTimes`: Must contain 1-5 time slots in ISO datetime format
- All fields except `additionalNotes` are required

**Request Example:**

```http
POST /api/supervisors/appointments/reschedule-request
Headers:
  X-User-Id: 5001
  Content-Type: application/json

Body:
{
  "appointmentId": 2001,
  "caseId": 3001,
  "patientId": 101,
  "preferredTimes": [
    "2026-01-16T10:00:00",
    "2026-01-16T14:00:00"
  ],
  "reason": "Patient has a conflict with the original time",
  "additionalNotes": "Patient prefers morning slots"
}
```

**Response Example (201 Created):**

```json
{
  "success": true,
  "message": "Reschedule request created successfully",
  "data": {
    "id": 7001,
    "caseId": 3001,
    "appointmentId": 2001,
    "requestedBy": "SUPERVISOR",
    "reason": "Patient has a conflict with the original time",
    "preferredTimes": "2026-01-16T10:00:00,2026-01-16T14:00:00",
    "status": "PENDING",
    "createdAt": "2026-01-08T14:30:00",
    "updatedAt": "2026-01-08T14:30:00",
    "patientId": 101,
    "currentTime": "2026-01-15T10:00:00",
    "requestedTime": null,
    "approvedTime": null,
    "doctorResponse": null,
    "requestedBySupervisorId": 5001,
    "respondedAt": null
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 8. Get Reschedule Requests

Retrieves all reschedule requests for assigned patients.

**Endpoint:** `GET /api/supervisors/appointments/reschedule-requests`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patientId | Long | No | Filter by patient ID |

**Request Example:**

```http
GET /api/supervisors/appointments/reschedule-requests?patientId=101
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 7001,
      "caseId": 3001,
      "appointmentId": 2001,
      "requestedBy": "SUPERVISOR",
      "reason": "Patient has a conflict with the original time",
      "preferredTimes": "2026-01-16T10:00:00,2026-01-16T14:00:00",
      "status": "PENDING",
      "createdAt": "2026-01-08T14:30:00",
      "updatedAt": "2026-01-08T14:30:00",
      "patientId": 101,
      "requestedBySupervisorId": 5001
    }
  ],
  "timestamp": "2026-01-08T14:35:00",
  "errorCode": null
}
```

---

### 9. Get Case Reschedule Requests

Retrieves reschedule requests for a specific case.

**Endpoint:** `GET /api/supervisors/appointments/case/{caseId}/reschedule-requests`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| caseId | Long | Yes | Case ID |

**Request Example:**

```http
GET /api/supervisors/appointments/case/3001/reschedule-requests
Headers:
  X-User-Id: 5001
```

**Response:** Same structure as "Get Reschedule Requests"

---

### 10. Get Appointment Summary

Retrieves appointment statistics and summary for supervisor's patients.

**Endpoint:** `GET /api/supervisors/appointments/summary`

**Request Example:**

```http
GET /api/supervisors/appointments/summary
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": {
    "totalAppointments": 45,
    "upcomingAppointments": 12,
    "completedAppointments": 28,
    "cancelledAppointments": 3,
    "rescheduledAppointments": 2,
    "pendingRescheduleRequests": 1,
    "appointmentsByStatus": {
      "SCHEDULED": 8,
      "CONFIRMED": 4,
      "COMPLETED": 28,
      "CANCELLED": 3,
      "RESCHEDULED": 2
    },
    "appointmentsByPatient": {
      "101": 15,
      "102": 12,
      "103": 18
    }
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 11. Get Today's Appointments

Retrieves all appointments scheduled for today.

**Endpoint:** `GET /api/supervisors/appointments/today`

**Request Example:**

```http
GET /api/supervisors/appointments/today
Headers:
  X-User-Id: 5001
```

**Response:** Array of appointments (same structure as "Get All Appointments")

---

### 12. Get Appointments by Status

Retrieves appointments filtered by status.

**Endpoint:** `GET /api/supervisors/appointments/status/{status}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | AppointmentStatus | Yes | SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW |

**Request Example:**

```http
GET /api/supervisors/appointments/status/SCHEDULED
Headers:
  X-User-Id: 5001
```

**Response:** Array of appointments (same structure as "Get All Appointments")

---

## Payment Management APIs

**Base Path:** `/api/supervisors/payments`

### 1. Pay Consultation Fee

Pay consultation fee on behalf of patient using Stripe, PayPal, or Coupon.

**Endpoint:** `POST /api/supervisors/payments/pay`

**Request Body:**

```json
{
  "caseId": 3001,
  "patientId": 101,
  "doctorId": 4001,
  "appointmentId": 2001,
  "paymentMethod": "STRIPE",
  "amount": 150.00,
  "paymentIntentId": "pi_3ABC123def456GHI",
  "notes": "Payment for neurology consultation"
}
```

**Payment Method Options:**

1. **STRIPE Payment:**
```json
{
  "caseId": 3001,
  "patientId": 101,
  "doctorId": 4001,
  "appointmentId": 2001,
  "paymentMethod": "STRIPE",
  "amount": 150.00,
  "paymentIntentId": "pi_3ABC123def456GHI",
  "notes": "Stripe payment"
}
```

2. **PAYPAL Payment:**
```json
{
  "caseId": 3001,
  "patientId": 101,
  "doctorId": 4001,
  "paymentMethod": "PAYPAL",
  "amount": 150.00,
  "paypalOrderId": "ORDER-12345-67890",
  "notes": "PayPal payment"
}
```

3. **COUPON Payment:**
```json
{
  "caseId": 3001,
  "patientId": 101,
  "doctorId": 4001,
  "paymentMethod": "COUPON",
  "couponCode": "CONSULT-2024-ABC123",
  "notes": "Coupon redemption"
}
```

**Response Example:**

```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "success": true,
    "paymentId": 8001,
    "caseId": 3001,
    "patientId": 101,
    "doctorId": 4001,
    "supervisorId": 5001,
    "paymentMethod": "STRIPE",
    "amount": 150.00,
    "discountAmount": 0.00,
    "finalAmount": 150.00,
    "couponCode": null,
    "stripePaymentIntentId": "pi_3ABC123def456GHI",
    "stripeChargeId": "ch_3ABC123def456XYZ",
    "paypalOrderId": null,
    "transactionId": "TXN-20260108-143000-001",
    "receiptUrl": "https://stripe.com/receipts/12345",
    "processedAt": "2026-01-08T14:30:00",
    "message": "Payment successful",
    "errorDetails": null
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 2. Create Payment Intent

Create a Stripe payment intent for consultation fee.

**Endpoint:** `POST /api/supervisors/payments/create-payment-intent`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| caseId | Long | Yes | Case ID |
| patientId | Long | Yes | Patient ID |
| doctorId | Long | Yes | Doctor ID |

**Request Example:**

```http
POST /api/supervisors/payments/create-payment-intent?caseId=3001&patientId=101&doctorId=4001
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": {
    "paymentId": 8001,
    "paymentIntentId": "pi_3ABC123def456GHI",
    "clientSecret": "pi_3ABC123def456GHI_secret_XYZ789",
    "amount": 150.00,
    "currency": "USD",
    "status": "requires_payment_method",
    "caseId": 3001,
    "patientId": 101,
    "doctorId": 4001
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 3. Validate Coupon

Validate a coupon code before using it for payment.

**Endpoint:** `POST /api/supervisors/payments/validate-coupon`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| couponCode | String | Yes | Coupon code to validate |
| patientId | Long | Yes | Patient ID |
| caseId | Long | Yes | Case ID |

**Request Example:**

```http
POST /api/supervisors/payments/validate-coupon?couponCode=CONSULT-2024-ABC123&patientId=101&caseId=3001
Headers:
  X-User-Id: 5001
```

**Response Example (Valid Coupon):**

```json
{
  "success": true,
  "message": null,
  "data": {
    "valid": true,
    "couponId": 6001,
    "couponCode": "CONSULT-2024-ABC123",
    "discountType": "PERCENTAGE",
    "discountValue": 20.00,
    "discountAmount": 30.00,
    "remainingAmount": 120.00,
    "originalAmount": 150.00,
    "expiresAt": "2026-02-01T23:59:59",
    "patientId": 101,
    "message": "Coupon is valid"
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

**Response Example (Invalid Coupon):**

```json
{
  "success": true,
  "message": null,
  "data": {
    "valid": false,
    "couponId": null,
    "couponCode": "INVALID-CODE",
    "discountType": null,
    "discountValue": null,
    "discountAmount": null,
    "remainingAmount": null,
    "originalAmount": 150.00,
    "expiresAt": null,
    "patientId": null,
    "message": "Coupon not found or invalid"
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 4. Redeem Coupon (Legacy)

**Note:** This is a legacy endpoint. Use "Pay Consultation Fee" with paymentMethod=COUPON instead.

Redeems a coupon to pay for a case consultation.

**Endpoint:** `POST /api/supervisors/payments/coupon/{caseId}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| caseId | Long | Yes | Case ID |

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patientId | Long | Yes | Patient ID |

**Request Body:**

```json
{
  "couponCode": "CONSULT-2024-ABC123"
}
```

**Request Example:**

```http
POST /api/supervisors/payments/coupon/3001?patientId=101
Headers:
  X-User-Id: 5001
  Content-Type: application/json

Body:
{
  "couponCode": "CONSULT-2024-ABC123"
}
```

**Response Example:**

```json
{
  "success": true,
  "message": "Coupon redeemed successfully",
  "data": {
    "paymentId": 8001,
    "caseId": 3001,
    "patientId": 101,
    "supervisorId": 5001,
    "paymentSource": "COUPON",
    "amount": 150.00,
    "currency": "USD",
    "status": "COMPLETED",
    "couponId": 6001,
    "couponCode": "CONSULT-2024-ABC123",
    "stripePaymentIntentId": null,
    "stripeClientSecret": null,
    "timestamp": "2026-01-08T14:30:00",
    "message": "Payment completed using coupon"
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 5. Get Coupon Summary

Retrieves coupon statistics for the supervisor.

**Endpoint:** `GET /api/supervisors/payments/coupons`

**Request Example:**

```http
GET /api/supervisors/payments/coupons
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": {
    "supervisorId": 5001,
    "totalCoupons": 50,
    "availableCoupons": 32,
    "usedCoupons": 15,
    "expiredCoupons": 2,
    "cancelledCoupons": 1,
    "couponsExpiringSoon": 5,
    "totalAvailableValue": 4800.00,
    "totalUsedValue": 2250.00,
    "totalExpiredValue": 300.00,
    "patientSummaries": [
      {
        "patientId": 101,
        "patientName": "John Doe",
        "availableCoupons": 8,
        "availableValue": 1200.00,
        "usedCoupons": 5,
        "expiredCoupons": 0
      },
      {
        "patientId": 102,
        "patientName": "Jane Smith",
        "availableCoupons": 12,
        "availableValue": 1800.00,
        "usedCoupons": 6,
        "expiredCoupons": 1
      }
    ]
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 6. Get Patient Coupons

Get available coupons for a specific patient.

**Endpoint:** `GET /api/supervisors/payments/coupons/patient/{patientId}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patientId | Long | Yes | Patient ID |

**Request Example:**

```http
GET /api/supervisors/payments/coupons/patient/101
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 6001,
      "couponCode": "CONSULT-2024-ABC123",
      "supervisorId": 5001,
      "patientId": 101,
      "amount": 150.00,
      "currency": "USD",
      "caseId": null,
      "status": "AVAILABLE",
      "issuedAt": "2026-01-01T10:00:00",
      "expiresAt": "2026-02-01T23:59:59",
      "usedAt": null,
      "cancelledAt": null,
      "issuedBy": 9001,
      "batchId": 5501,
      "batchCode": "BATCH-2024-001",
      "notes": "General consultation coupon",
      "cancellationReason": null,
      "isExpiringSoon": true,
      "daysUntilExpiry": 24,
      "createdAt": "2026-01-01T10:00:00",
      "updatedAt": "2026-01-01T10:00:00",
      "patientFirstName": "John",
      "patientLastName": "Doe",
      "supervisorFullName": "Dr. Emily Williams"
    }
  ],
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

### 7. Get All Available Coupons

Get all available coupons for the supervisor.

**Endpoint:** `GET /api/supervisors/payments/available-coupons`

**Request Example:**

```http
GET /api/supervisors/payments/available-coupons
Headers:
  X-User-Id: 5001
```

**Response:** Same structure as "Get Patient Coupons" but includes coupons for all patients

---

### 8. Get Expiring Coupons

Get coupons expiring within specified days.

**Endpoint:** `GET /api/supervisors/payments/coupons/expiring`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| days | Integer | No | Days until expiry (default: 7) |

**Request Example:**

```http
GET /api/supervisors/payments/coupons/expiring?days=14
Headers:
  X-User-Id: 5001
```

**Response:** Same structure as "Get Patient Coupons"

---

### 9. Get Payment History

**Note:** This endpoint is a placeholder for future implementation.

Retrieves payment history for the supervisor.

**Endpoint:** `GET /api/supervisors/payments/history`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patientId | Long | No | Filter by patient ID |

**Request Example:**

```http
GET /api/supervisors/payments/history?patientId=101
Headers:
  X-User-Id: 5001
```

**Response Example:**

```json
{
  "success": true,
  "message": "Payment history endpoint - to be implemented in Phase 5",
  "data": null,
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": null
}
```

---

## Data Models

### SupervisorAppointmentDto

```typescript
{
  id: number;                          // Appointment ID
  caseId: number;                      // Associated case ID
  caseTitle: string;                   // Title of the case
  doctor: DoctorDto;                   // Doctor information object
  doctorId: number;                    // Doctor ID
  doctorName: string;                  // Doctor full name
  patientId: number;                   // Patient ID
  patientName: string;                 // Patient full name
  patientEmail: string;                // Patient email
  consultationFee: number;             // Consultation fee amount
  scheduledTime: string;               // ISO datetime (YYYY-MM-DDTHH:mm:ss)
  duration: number;                    // Duration in minutes
  consultationType: ConsultationType;  // WHATSAPP, ZOOM, PHONE_CALL, IN_PERSON
  status: AppointmentStatus;           // Appointment status
  rescheduleCount: number;             // Number of times rescheduled
  meetingLink: string | null;          // Video meeting link (if applicable)
  meetingId: string | null;            // Meeting ID (if applicable)
  rescheduledFrom: string | null;      // Original scheduled time (ISO datetime)
  rescheduleReason: string | null;     // Reason for reschedule
  completedAt: string | null;          // Completion datetime (ISO)
  supervisorId: number;                // Supervisor ID
  isSupervisorManaged: boolean;        // Whether managed by supervisor
  hasPendingRescheduleRequest: boolean;// Has pending reschedule request
  rescheduleRequestId: number | null;  // Related reschedule request ID
  specialization: string;              // Doctor's specialization
  createdAt: string;                   // ISO datetime
  updatedAt: string;                   // ISO datetime
}
```

### AcceptAppointmentDto (Request)

```typescript
{
  caseId: number;        // Required
  patientId: number;     // Required
  notes?: string;        // Optional notes
}
```

### SupervisorRescheduleRequestDto (Request)

```typescript
{
  appointmentId: number;              // Required
  caseId: number;                     // Required
  patientId: number;                  // Required
  preferredTimes: string[];           // Required: 1-5 ISO datetime strings
  reason: string;                     // Required
  additionalNotes?: string;           // Optional
}
```

### RescheduleRequestResponseDto

```typescript
{
  id: number;
  caseId: number;
  appointmentId: number;
  requestedBy: string;                // "PATIENT" or "SUPERVISOR"
  reason: string;
  preferredTimes: string;             // Comma-separated ISO datetimes
  status: string;                     // "PENDING", "APPROVED", "REJECTED"
  createdAt: string;                  // ISO datetime
  updatedAt: string;                  // ISO datetime
  patientId: number;
  currentTime: string | null;         // ISO datetime
  requestedTime: string | null;       // ISO datetime
  approvedTime: string | null;        // ISO datetime
  doctorResponse: string | null;
  requestedBySupervisorId: number | null;
  respondedAt: string | null;         // ISO datetime
}
```

### AppointmentSummaryDto

```typescript
{
  totalAppointments: number;
  upcomingAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  rescheduledAppointments: number;
  pendingRescheduleRequests: number;
  appointmentsByStatus: {
    [status: string]: number;         // Count by status
  };
  appointmentsByPatient: {
    [patientId: string]: number;      // Count by patient
  };
}
```

### SupervisorPayConsultationDto (Request)

```typescript
{
  caseId: number;                     // Required
  patientId: number;                  // Required
  doctorId: number;                   // Required
  appointmentId?: number;             // Optional
  paymentMethod: PaymentMethod;       // Required: STRIPE, PAYPAL, or COUPON
  amount?: number;                    // Required for STRIPE/PAYPAL
  couponCode?: string;                // Required when paymentMethod = COUPON
  paymentMethodId?: string;           // Stripe payment method ID
  paymentIntentId?: string;           // Stripe payment intent ID
  paypalOrderId?: string;             // PayPal order ID
  notes?: string;                     // Optional payment notes
}
```

### SupervisorPaymentResponseDto

```typescript
{
  success: boolean;
  paymentId: number;
  caseId: number;
  patientId: number;
  doctorId: number;
  supervisorId: number;
  paymentMethod: string;              // "STRIPE", "PAYPAL", "COUPON"
  amount: number;                     // Original consultation fee
  discountAmount: number;             // Discount applied (if any)
  finalAmount: number;                // Final amount charged
  couponCode: string | null;          // Coupon code used (if applicable)
  stripePaymentIntentId: string | null;
  stripeChargeId: string | null;
  paypalOrderId: string | null;
  transactionId: string;              // Internal transaction ID
  receiptUrl: string | null;          // Receipt URL
  processedAt: string;                // ISO datetime
  message: string;
  errorDetails: string | null;
}
```

### PaymentIntentDto

```typescript
{
  paymentId: number;
  paymentIntentId: string;            // Stripe payment intent ID
  clientSecret: string;               // Client secret for frontend
  amount: number;
  currency: string;                   // e.g., "USD"
  status: string;                     // Stripe status
  caseId: number;
  patientId: number;
  doctorId: number;
}
```

### CouponValidationResponseDto

```typescript
{
  valid: boolean;
  couponId: number | null;
  couponCode: string;
  discountType: DiscountType | null;  // PERCENTAGE, FIXED_AMOUNT, FULL_COVERAGE
  discountValue: number | null;       // Percentage or fixed amount
  discountAmount: number | null;      // Calculated discount
  remainingAmount: number | null;     // Amount after discount
  originalAmount: number;             // Original consultation fee
  expiresAt: string | null;           // ISO datetime
  patientId: number | null;
  message: string;
}
```

### CouponDto

```typescript
{
  id: number;
  couponCode: string;
  supervisorId: number;
  patientId: number | null;
  amount: number;
  currency: string;
  caseId: number | null;
  status: CouponStatus;               // AVAILABLE, USED, EXPIRED, CANCELLED
  issuedAt: string;                   // ISO datetime
  expiresAt: string;                  // ISO datetime
  usedAt: string | null;              // ISO datetime
  cancelledAt: string | null;         // ISO datetime
  issuedBy: number;
  batchId: number | null;
  batchCode: string | null;
  notes: string | null;
  cancellationReason: string | null;
  isExpiringSoon: boolean;
  daysUntilExpiry: number;
  createdAt: string;                  // ISO datetime
  updatedAt: string;                  // ISO datetime
  patientFirstName: string | null;
  patientLastName: string | null;
  supervisorFullName: string | null;
}
```

### CouponSummaryDto

```typescript
{
  supervisorId: number;
  totalCoupons: number;
  availableCoupons: number;
  usedCoupons: number;
  expiredCoupons: number;
  cancelledCoupons: number;
  couponsExpiringSoon: number;
  totalAvailableValue: number;
  totalUsedValue: number;
  totalExpiredValue: number;
  patientSummaries: Array<{
    patientId: number;
    patientName: string;
    availableCoupons: number;
    availableValue: number;
    usedCoupons: number;
    expiredCoupons: number;
  }>;
}
```

### ApiResponse<T> (Generic Response Wrapper)

```typescript
{
  success: boolean;
  message: string | null;
  data: T;                            // Generic data payload
  timestamp: string;                  // ISO datetime
  errorCode: string | null;
}
```

---

## Enumerations

### AppointmentStatus

```typescript
enum AppointmentStatus {
  SCHEDULED = "SCHEDULED",
  CONFIRMED = "CONFIRMED",
  COMPLETED = "COMPLETED",
  CANCELLED = "CANCELLED",
  RESCHEDULED = "RESCHEDULED",
  NO_SHOW = "NO_SHOW"
}
```

### ConsultationType

```typescript
enum ConsultationType {
  WHATSAPP = "WHATSAPP",
  ZOOM = "ZOOM",
  PHONE_CALL = "PHONE_CALL",
  IN_PERSON = "IN_PERSON"
}
```

### PaymentMethod

```typescript
enum PaymentMethod {
  CREDIT_CARD = "CREDIT_CARD",
  PAYPAL = "PAYPAL",
  BANK_TRANSFER = "BANK_TRANSFER",
  STRIPE = "STRIPE",
  COUPON = "COUPON"
}
```

### CouponStatus

```typescript
enum CouponStatus {
  AVAILABLE = "AVAILABLE",    // Coupon is available for use
  USED = "USED",             // Coupon has been used
  EXPIRED = "EXPIRED",       // Coupon has expired
"success": false,
    "data": null,
    "timestamp": "2026-01-08T14:30:00",
    "errorCode": "ERROR_CODE"
}
```

### Common HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 OK | Request successful |
| 201 Created | Resource created successfully (e.g., reschedule request) |
| 400 Bad Request | Invalid request parameters or validation error |
| 401 Unauthorized | Missing or invalid authentication (X-User-Id) |
| 403 Forbidden | User does not have permission to access this resource |
| 404 Not Found | Resource not found (appointment, patient, case, etc.) |
| 409 Conflict | Resource conflict (e.g., coupon already used) |
| 500 Internal Server Error | Server-side error |

### Common Error Scenarios

**1. Unauthorized Access:**
```json
{
  "success": false,
  "message": "Unauthorized access to appointment",
  "data": null,
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": "UNAUTHORIZED"
}
```

**2. Validation Error:**
```json
{
  "success": false,
  "message": "Case ID is required",
  "data": null,
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": "VALIDATION_ERROR"
}
```

**3. Resource Not Found:**
```json
{
  "success": false,
  "message": "Appointment not found",
  "data": null,
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": "NOT_FOUND"
}
```

**4. Invalid Coupon:**
```json
{
  "success": false,
  "message": "Coupon has expired",
  "data": null,
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": "COUPON_EXPIRED"
}
```

**5. Payment Failed:**
```json
{
  "success": false,
  "message": "Payment processing failed",
  "data": {
    "errorDetails": "Insufficient funds in account"
  },
  "timestamp": "2026-01-08T14:30:00",
  "errorCode": "PAYMENT_FAILED"
}
```

---

## Frontend Implementation Notes

### 1. Authentication Header

Always include the supervisor's user ID in the request headers:

```javascript
const headers = {
    'X-User-Id': supervisorUserId,
    'Content-Type': 'application/json'
};
```

### 2. Date Formatting

- All dates are in ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
- For date-only filters, use: `YYYY-MM-DD`

### 3. Payment Flow

**Recommended payment flow:**

1. **Get appointment details** to retrieve consultation fee
2. **Validate coupon** (if using coupon) to show discount preview
3. **Create payment intent** (if using Stripe) to get client secret
4. **Process payment** on frontend (Stripe/PayPal)
5. **Call pay endpoint** with payment confirmation details

**Example Stripe Payment Flow:**

```javascript
// Step 1: Create payment intent
const intentResponse = await fetch(
    `/api/supervisors/payments/create-payment-intent?caseId=${caseId}&patientId=${patientId}&doctorId=${doctorId}`,
    { headers, method: 'POST' }
);
const { clientSecret } = intentResponse.data;

// Step 2: Confirm payment with Stripe on frontend
const { paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
    payment_method: paymentMethodId
});

// Step 3: Complete payment on backend
await fetch('/api/supervisors/payments/pay', {
    method: 'POST',
    headers,
    body: JSON.stringify({
        caseId,
        patientId,
        doctorId,
        paymentMethod: 'STRIPE',
        amount: consultationFee,
        paymentIntentId: paymentIntent.id
    })
});
```

### 4. Filtering Appointments

Use query parameters to build dynamic filters:

```javascript
const buildQueryString = (filters) => {
    const params = new URLSearchParams();

    if (filters.patientId) params.append('patientId', filters.patientId);
    if (filters.status) params.append('status', filters.status);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);

    return params.toString();
};

const url = `/api/supervisors/appointments?${buildQueryString(filters)}`;
```

### 5. Error Handling

```javascript
const handleApiResponse = async (response) => {
    const data = await response.json();

    if (!data.success) {
        throw new Error(data.message || 'Request failed');
    }

    return data.data;
};
```
CANCELLED = "CANCELLED"    // Coupon was cancelled/revoked
}
```

### DiscountType

```typescript
enum DiscountType {
PERCENTAGE = "PERCENTAGE",        // Percentage discount (e.g., 20% off)
FIXED_AMOUNT = "FIXED_AMOUNT",   // Fixed amount discount (e.g., $50 off)
FULL_COVERAGE = "FULL_COVERAGE"  // Full coverage - covers entire fee
}
```

---

## Error Handling

All endpoints follow a consistent error response format:

### Error Response Structure

```json
{
"message": "Error description",

---

## Change Log

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-08 | Initial API documentation |

---

## Support

For questions or issues with these APIs, please contact the backend development team or refer to the main Supervisor Service documentation.