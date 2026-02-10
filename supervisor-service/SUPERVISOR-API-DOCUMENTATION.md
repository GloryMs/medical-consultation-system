# Supervisor Service API Documentation

**Base URL:** `http://localhost:8085` (or your configured URL)
**Version:** 1.0
**Last Updated:** 2026-01-01

---

## 📋 Table of Contents

1. [Authentication](#authentication)
2. [Profile Management](#profile-management)
3. [Patient Management](#patient-management)
4. [Case Management](#case-management)
5. [Dashboard & Analytics](#dashboard--analytics)
6. [Payment & Coupons](#payment--coupons)
7. [Settings](#settings)
8. [Common Response Format](#common-response-format)
9. [Error Codes](#error-codes)

---

## 🔐 Authentication

All endpoints require authentication via JWT token and user ID header.

**Required Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <supervisor_user_id>
```

---

## 👤 Profile Management

### 1. Create Supervisor Profile

```http
POST /api/supervisors/profile
```

**Request Body:**
```json
{
  "fullName": "Dr. John Smith",
  "email": "john.smith@example.com",
  "phoneNumber": "+1234567890",
  "licenseNumber": "LIC123456",
  "specialization": "Family Medicine",
  "yearsOfExperience": 10,
  "organization": "City Medical Center",
  "bio": "Experienced medical supervisor...",
  "emergencyContactName": "Jane Smith",
  "emergencyContactPhone": "+1234567891"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Profile created successfully. Awaiting admin verification.",
  "data": {
    "id": 1,
    "userId": 100,
    "fullName": "Dr. John Smith",
    "email": "john.smith@example.com",
    "verificationStatus": "PENDING",
    "maxPatientsLimit": 20,
    "maxActiveCasesPerPatient": 3
  }
}
```

---

### 2. Get Supervisor Profile

```http
GET /api/supervisors/profile
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 100,
    "fullName": "Dr. John Smith",
    "email": "john.smith@example.com",
    "phoneNumber": "+1234567890",
    "licenseNumber": "LIC123456",
    "specialization": "Family Medicine",
    "verificationStatus": "VERIFIED",
    "isActive": true,
    "maxPatientsLimit": 20,
    "maxActiveCasesPerPatient": 3
  }
}
```

---

### 3. Update Supervisor Profile

```http
PUT /api/supervisors/profile
```

**Request Body:**
```json
{
  "fullName": "Dr. John Smith",
  "phoneNumber": "+1234567890",
  "bio": "Updated bio...",
  "organization": "New Medical Center"
}
```

---

### 4. Upload License Document

```http
POST /api/supervisors/profile/license-document
Content-Type: multipart/form-data
```

**Form Data:**
```
file: <PDF/Image file>
```

**Response:**
```json
{
  "success": true,
  "message": "License document uploaded successfully",
  "data": "/uploads/licenses/123456.pdf"
}
```

---

### 5. Delete Profile

```http
DELETE /api/supervisors/profile
```

---

## 👥 Patient Management

### 1. Get All Assigned Patients

```http
GET /api/supervisors/patients
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "patientId": 50,
      "patientName": "Alice Johnson",
      "patientEmail": "alice@example.com",
      "assignedAt": "2024-01-15T10:30:00",
      "isActive": true,
      "activeCasesCount": 2,
      "totalCasesCount": 5,
      "notes": "Special care required"
    }
  ]
}
```

---

### 2. Assign Patient

```http
POST /api/supervisors/patients?patientId=50&notes=Special%20care
```

**Query Parameters:**
- `patientId` (required): Patient ID to assign
- `notes` (optional): Assignment notes

**Response:**
```json
{
  "success": true,
  "message": "Patient assigned successfully",
  "data": {
    "id": 1,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "assignedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 3. Get Patient Assignment Details

```http
GET /api/supervisors/patients/{patientId}
```

---

### 4. Remove Patient Assignment

```http
DELETE /api/supervisors/patients/{patientId}?reason=Patient%20transferred
```

**Query Parameters:**
- `reason` (optional): Reason for removal

---

### 5. Get Patient IDs

```http
GET /api/supervisors/patients/ids
```

**Response:**
```json
{
  "success": true,
  "data": [50, 51, 52, 53]
}
```

---

## 📋 Case Management

### 1. Submit Case for Patient

```http
POST /api/supervisors/cases/patient/{patientId}
```

**Request Body:**
```json
{
  "caseTitle": "Chronic Back Pain",
  "description": "Patient experiencing severe back pain for 3 weeks...",
  "primaryDiseaseCode": "M54.5",
  "secondaryDiseaseCodes": ["M25.5"],
  "symptomCodes": ["R52", "R53"],
  "currentMedicationCodes": ["N02BE01"],
  "requiredSpecialization": "ORTHOPEDICS",
  "secondarySpecializations": ["PHYSIOTHERAPY"],
  "urgencyLevel": "MEDIUM",
  "complexity": "MODERATE",
  "requiresSecondOpinion": true,
  "minDoctorsRequired": 2,
  "maxDoctorsAllowed": 3,
  "dependentId": null
}
```

**Response:**
```json
{
  "success": true,
  "message": "Case submitted successfully",
  "data": {
    "id": 1001,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "caseTitle": "Chronic Back Pain",
    "status": "PENDING",
    "urgencyLevel": "MEDIUM",
    "requiredSpecialization": "ORTHOPEDICS",
    "createdAt": "2024-01-15T10:30:00",
    "submittedAt": "2024-01-15T10:30:00"
  }
}
```

**Note:** Files can be uploaded separately after case creation using patient-service attachment endpoint.

---

### 2. Get All Cases

```http
GET /api/supervisors/cases
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "patientId": 50,
      "patientName": "Alice Johnson",
      "caseTitle": "Chronic Back Pain",
      "status": "IN_PROGRESS",
      "urgencyLevel": "MEDIUM",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

---

### 3. Get Cases for Specific Patient

```http
GET /api/supervisors/cases/patient/{patientId}
```

---

### 4. Get Case Details

```http
GET /api/supervisors/cases/{caseId}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "caseTitle": "Chronic Back Pain",
    "description": "Patient experiencing severe back pain...",
    "status": "IN_PROGRESS",
    "urgencyLevel": "MEDIUM",
    "complexity": "MODERATE",
    "requiredSpecialization": "ORTHOPEDICS",
    "primaryDiseaseCode": "M54.5",
    "paymentStatus": "PENDING",
    "consultationFee": 150.00,
    "createdAt": "2024-01-15T10:30:00",
    "submittedAt": "2024-01-15T10:30:00",
    "firstAssignedAt": "2024-01-15T11:00:00"
  }
}
```

---

### 5. Update Case

```http
PUT /api/supervisors/cases/{caseId}
```

**Request Body:**
```json
{
  "caseTitle": "Updated Case Title",
  "description": "Updated description...",
  "urgencyLevel": "HIGH",
  "complexity": "COMPLEX"
}
```

---

### 6. Cancel Case

```http
PUT /api/supervisors/cases/{caseId}/cancel?reason=Patient%20withdrew
```

**Query Parameters:**
- `reason` (required): Cancellation reason

---

### 7. Get Patient Info (for case submission)

```http
GET /api/supervisors/cases/patient/{patientId}/info
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 50,
    "userId": 200,
    "fullName": "Alice Johnson",
    "email": "alice@example.com",
    "phoneNumber": "+1234567890",
    "dateOfBirth": "1990-05-15",
    "gender": "FEMALE",
    "subscriptionStatus": "ACTIVE"
  }
}
```

---

## 📊 Dashboard & Analytics

### 1. Get Dashboard Statistics

```http
GET /api/supervisors/dashboard/statistics
```

**Response:**
```json
{
  "success": true,
  "data": {
    "supervisorId": 1,
    "supervisorName": "Dr. John Smith",
    "verificationStatus": "VERIFIED",
    "activePatientCount": 15,
    "maxPatientsLimit": 20,
    "totalCasesSubmitted": 45,
    "activeCases": 12,
    "completedCases": 30,
    "totalAppointments": 40,
    "upcomingAppointments": 5,
    "completedAppointments": 35,
    "totalCouponsIssued": 50,
    "availableCoupons": 20,
    "usedCoupons": 30,
    "totalCouponValue": 5000.00,
    "totalPaymentsProcessed": 35,
    "lastActivityAt": "2024-01-15T10:30:00"
  }
}
```

---

### 2. Get Recent Activity

```http
GET /api/supervisors/dashboard/activity?limit=20
```

**Query Parameters:**
- `limit` (optional, default: 20): Number of activities to retrieve

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "activityType": "CASE_SUBMITTED",
      "title": "Case Submitted",
      "description": "Case submitted for patient: Alice Johnson - Chronic Back Pain",
      "relatedEntityId": 1001,
      "relatedEntityType": "CASE",
      "timestamp": "2024-01-15T10:30:00"
    }
  ]
}
```

---

### 3. Get Performance Metrics

```http
GET /api/supervisors/dashboard/metrics
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Performance metrics endpoint - to be fully implemented",
    "note": "Requires data from patient-service, doctor-service, and payment-service"
  }
}
```

---

## 💰 Payment & Coupons

### 1. Redeem Coupon for Case

```http
POST /api/supervisors/payments/coupon/{caseId}?patientId=50
```

**Query Parameters:**
- `patientId` (required): Patient ID

**Request Body:**
```json
{
  "couponCode": "SAVE20"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Coupon redeemed successfully",
  "data": {
    "success": true,
    "caseId": 1001,
    "couponCode": "SAVE20",
    "discountAmount": 30.00,
    "originalAmount": 150.00,
    "finalAmount": 120.00,
    "paymentId": 5001,
    "processedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 2. Get Coupon Summary

```http
GET /api/supervisors/payments/coupons
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalCoupons": 50,
    "availableCoupons": 20,
    "usedCoupons": 25,
    "expiredCoupons": 5,
    "totalAvailableValue": 5000.00
  }
}
```

---

### 3. Get Available Coupons for Patient

```http
GET /api/supervisors/payments/coupons/patient/{patientId}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "couponCode": "SAVE20",
      "discountAmount": 30.00,
      "discountPercentage": 20,
      "status": "AVAILABLE",
      "issuedAt": "2024-01-10T09:00:00",
      "expiresAt": "2024-02-10T09:00:00",
      "patientId": 50
    }
  ]
}
```

---

### 4. Get Payment History

```http
GET /api/supervisors/payments/history?patientId=50
```

**Query Parameters:**
- `patientId` (optional): Filter by patient ID

**Note:** This endpoint is currently a placeholder and returns a message indicating it will be implemented in a future phase.

---

## ⚙️ Settings

### 1. Get Settings

```http
GET /api/supervisors/settings
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "supervisorId": 1,
    "emailNotifications": true,
    "smsNotifications": false,
    "pushNotifications": true,
    "newCaseAssignmentNotification": true,
    "appointmentRemindersNotification": true,
    "caseStatusUpdateNotification": true,
    "couponIssuedNotification": true,
    "couponExpiringNotification": true,
    "preferredLanguage": "en",
    "timezone": "America/New_York",
    "theme": "light"
  }
}
```

---

### 2. Update Settings

```http
PUT /api/supervisors/settings
```

**Request Body:**
```json
{
  "emailNotifications": true,
  "smsNotifications": true,
  "pushNotifications": true,
  "newCaseAssignmentNotification": true,
  "appointmentRemindersNotification": false,
  "caseStatusUpdateNotification": true,
  "couponIssuedNotification": true,
  "couponExpiringNotification": true,
  "preferredLanguage": "en",
  "timezone": "America/New_York",
  "theme": "dark"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Settings updated successfully",
  "data": {
    "id": 1,
    "supervisorId": 1,
    "emailNotifications": true,
    "theme": "dark"
  }
}
```

---

## 📦 Common Response Format

All API responses follow this format:

```json
{
  "success": true | false,
  "message": "Optional message",
  "data": <Response data> | null
}
```

---

## ❌ Error Codes

| HTTP Status | Error Type | Description |
|------------|------------|-------------|
| 400 | Bad Request | Invalid request data or validation error |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | Insufficient permissions or inactive supervisor |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., patient already assigned) |
| 500 | Internal Server Error | Server-side error |

**Error Response Format:**
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

---

## 🔑 Common Field Enums

### Verification Status
- `PENDING` - Awaiting verification
- `VERIFIED` - Verified and active
- `REJECTED` - Verification rejected
- `SUSPENDED` - Account suspended

### Case Status
- `SUBMITTED` - Case submitted
- `PENDING` - Awaiting assignment
- `ASSIGNED` - Assigned to doctor
- `IN_PROGRESS` - Doctor working on case
- `COMPLETED` - Case completed
- `CLOSED` - Case closed

### Urgency Level
- `LOW` - Low urgency
- `MEDIUM` - Medium urgency
- `HIGH` - High urgency
- `CRITICAL` - Critical urgency

### Case Complexity
- `SIMPLE` - Simple case
- `MODERATE` - Moderate complexity
- `COMPLEX` - Complex case
- `VERY_COMPLEX` - Very complex case

### Payment Status
- `PENDING` - Payment pending
- `COMPLETED` - Payment completed
- `FAILED` - Payment failed
- `REFUNDED` - Payment refunded

---

## 📝 Notes for Frontend Team

1. **Always include** `X-User-Id` header in all requests
2. **File uploads** for case attachments should be done via separate patient-service endpoint after case creation
3. **Polling**: Dashboard statistics can be polled every 30-60 seconds for real-time updates
4. **Pagination**: Currently not implemented, will be added in future versions
5. **Date format**: All dates are in ISO 8601 format (e.g., `2024-01-15T10:30:00`)
6. **Amounts**: All monetary amounts are in decimal format (e.g., `150.00`)

---

## 🚀 Quick Start Example (JavaScript/TypeScript)

```javascript
// Example: Get Dashboard Statistics
const response = await fetch('http://localhost:8085/api/supervisors/dashboard/statistics', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-User-Id': userId,
    'Content-Type': 'application/json'
  }
});

const result = await response.json();
if (result.success) {
  console.log('Dashboard data:', result.data);
}

// Example: Submit Case
const caseData = {
  caseTitle: "Chronic Back Pain",
  description: "Patient experiencing severe back pain...",
  requiredSpecialization: "ORTHOPEDICS",
  urgencyLevel: "MEDIUM"
};

const submitResponse = await fetch(
  `http://localhost:8085/api/supervisors/cases/patient/${patientId}`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-User-Id': userId,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(caseData)
  }
);

const submitResult = await submitResponse.json();
if (submitResult.success) {
  console.log('Case created:', submitResult.data);
}
```

---

**For questions or issues, contact the backend team.**

**Last Updated:** January 1, 2026