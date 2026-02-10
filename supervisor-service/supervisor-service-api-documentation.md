# Supervisor Service API Documentation

**Base URL:** `http://localhost:8085`
**Version:** 1.0
**Last Updated:** 2026-01-04

---

## Table of Contents

1. [Authentication](#authentication)
2. [Profile Management](#profile-management)
3. [Patient Management](#patient-management)
4. [Case Management](#case-management)
5. [Dashboard & Analytics](#dashboard--analytics)
6. [Payment & Coupons](#payment--coupons)
7. [Settings](#settings)
8. [Common Enums](#common-enums)
9. [Error Responses](#error-responses)

---

## Authentication

All endpoints require authentication via JWT token and user ID header.

**Required Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <supervisor_user_id>
Content-Type: application/json
```

---

## Profile Management

### 1. Create Supervisor Profile

**Endpoint:** `POST /api/supervisors/profile`

**Description:** Creates a new supervisor profile with PENDING verification status

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Request Body:**
```json
{
  "fullName": "Dr. John Smith",
  "organizationName": "City Medical Center",
  "organizationType": "Hospital",
  "licenseNumber": "LIC123456",
  "phoneNumber": "+1234567890",
  "email": "john.smith@example.com",
  "address": "123 Main St",
  "city": "New York",
  "country": "USA"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Profile created successfully. Awaiting admin verification.",
  "data": {
    "id": 1,
    "userId": 100,
    "fullName": "Dr. John Smith",
    "organizationName": "City Medical Center",
    "organizationType": "Hospital",
    "licenseNumber": "LIC123456",
    "phoneNumber": "+1234567890",
    "email": "john.smith@example.com",
    "address": "123 Main St",
    "city": "New York",
    "country": "USA",
    "verificationStatus": "PENDING",
    "maxPatientsLimit": 20,
    "maxActiveCasesPerPatient": 3,
    "isActive": false,
    "createdAt": "2026-01-04T10:30:00"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Full name is required",
  "data": null
}
```

---

### 2. Get Supervisor Profile

**Endpoint:** `GET /api/supervisors/profile`

**Description:** Retrieves the authenticated supervisor's profile

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "userId": 100,
    "fullName": "Dr. John Smith",
    "organizationName": "City Medical Center",
    "organizationType": "Hospital",
    "licenseNumber": "LIC123456",
    "phoneNumber": "+1234567890",
    "email": "john.smith@example.com",
    "address": "123 Main St",
    "city": "New York",
    "country": "USA",
    "verificationStatus": "VERIFIED",
    "maxPatientsLimit": 20,
    "maxActiveCasesPerPatient": 3,
    "isActive": true,
    "createdAt": "2026-01-04T10:30:00",
    "updatedAt": "2026-01-04T11:00:00"
  }
}
```

**Error Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Supervisor profile not found",
  "data": null
}
```

---

### 3. Update Supervisor Profile

**Endpoint:** `PUT /api/supervisors/profile`

**Description:** Updates supervisor profile information

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Request Body (All fields optional):**
```json
{
  "fullName": "Dr. John M. Smith",
  "phoneNumber": "+1234567899",
  "address": "456 Oak Avenue",
  "city": "Brooklyn",
  "organizationName": "Brooklyn Medical Center"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "userId": 100,
    "fullName": "Dr. John M. Smith",
    "phoneNumber": "+1234567899",
    "address": "456 Oak Avenue",
    "city": "Brooklyn",
    "organizationName": "Brooklyn Medical Center"
  }
}
```

---

### 4. Upload License Document

**Endpoint:** `POST /api/supervisors/profile/license-document`

**Description:** Uploads professional license document for verification

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: multipart/form-data
```

**Request Body (multipart/form-data):**
```
file: <PDF/Image file>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "License document uploaded successfully",
  "data": "/uploads/licenses/supervisor_100_license.pdf"
}
```

---

### 5. Delete Supervisor Profile

**Endpoint:** `DELETE /api/supervisors/profile`

**Description:** Soft deletes the supervisor profile

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile deleted successfully",
  "data": null
}
```

---

## Patient Management

### 1. Get All Assigned Patients

**Endpoint:** `GET /api/supervisors/patients`

**Description:** Retrieves all patients assigned to the supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "supervisorId": 1,
      "patientId": 50,
      "patientName": "Alice Johnson",
      "patientFirstName": "Alice",
      "patientLastName": "Johnson",
      "patientEmail": "alice@example.com",
      "patientPhoneNumber": "+1234567890",
      "assignmentStatus": "ACTIVE",
      "assignedAt": "2026-01-04T10:00:00",
      "assignedBy": 100,
      "assignmentNotes": "Requires regular monitoring",
      "createdAt": "2026-01-04T10:00:00",
      "updatedAt": "2026-01-04T10:00:00"
    },
    {
      "id": 2,
      "supervisorId": 1,
      "patientId": 51,
      "patientName": "Bob Williams",
      "patientFirstName": "Bob",
      "patientLastName": "Williams",
      "patientEmail": "bob@example.com",
      "patientPhoneNumber": "+1234567891",
      "assignmentStatus": "ACTIVE",
      "assignedAt": "2026-01-03T14:30:00",
      "assignedBy": 100,
      "assignmentNotes": null,
      "createdAt": "2026-01-03T14:30:00",
      "updatedAt": "2026-01-03T14:30:00"
    }
  ]
}
```

---

### 2. Assign Patient by ID

**Endpoint:** `POST /api/supervisors/patients`

**Description:** Assigns an existing patient to the supervisor using patient ID

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Query Parameters:**
- `patientId` (required): Patient ID to assign
- `notes` (optional): Assignment notes

**Example:** `POST /api/supervisors/patients?patientId=50&notes=High%20priority%20patient`

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Patient assigned successfully",
  "data": {
    "id": 3,
    "supervisorId": 1,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "patientEmail": "alice@example.com",
    "assignmentStatus": "ACTIVE",
    "assignedAt": "2026-01-04T15:00:00",
    "assignedBy": 100,
    "assignmentNotes": "High priority patient",
    "createdAt": "2026-01-04T15:00:00"
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "success": false,
  "message": "Patient is already assigned to this supervisor",
  "data": null
}
```

---

### 3. Create Patient and Assign

**Endpoint:** `POST /api/supervisors/patients/create-and-assign`

**Description:** Creates a new patient account and assigns to supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Request Body:**
```json
{
  "fullName": "Sarah Martinez",
  "email": "sarah.martinez@example.com",
  "phoneNumber": "+1234567892",
  "dateOfBirth": "1985-05-15",
  "gender": "FEMALE",
  "address": "789 Pine St",
  "city": "Los Angeles",
  "country": "USA",
  "state": "CA",
  "zipCode": "90001",
  "bloodType": "A_POSITIVE",
  "allergies": "Penicillin",
  "chronicConditions": "Diabetes Type 2",
  "currentMedications": "Metformin",
  "emergencyContactName": "Carlos Martinez",
  "emergencyContactPhone": "+1234567893",
  "emergencyContactRelationship": "Spouse",
  "notes": "New patient referral",
  "autoAssignToSupervisor": true
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Patient created and assigned successfully",
  "data": {
    "id": 4,
    "supervisorId": 1,
    "patientId": 52,
    "patientName": "Sarah Martinez",
    "patientEmail": "sarah.martinez@example.com",
    "patientPhoneNumber": "+1234567892",
    "assignmentStatus": "ACTIVE",
    "assignedAt": "2026-01-04T16:00:00",
    "assignedBy": 100,
    "assignmentNotes": "New patient referral",
    "createdAt": "2026-01-04T16:00:00"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Email is required",
  "data": null
}
```

---

### 4. Assign Patient by Email

**Endpoint:** `POST /api/supervisors/patients/assign-by-email`

**Description:** Assigns an existing patient to supervisor using patient's email

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Request Body:**
```json
{
  "patientEmail": "alice@example.com",
  "assignmentNotes": "Patient requested transfer",
  "patientFullName": "Alice Johnson"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Patient assigned successfully",
  "data": {
    "id": 5,
    "supervisorId": 1,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "patientEmail": "alice@example.com",
    "assignmentStatus": "ACTIVE",
    "assignedAt": "2026-01-04T17:00:00",
    "assignedBy": 100,
    "assignmentNotes": "Patient requested transfer",
    "createdAt": "2026-01-04T17:00:00"
  }
}
```

**Error Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Patient not found with email: alice@example.com",
  "data": null
}
```

---

### 5. Get Patient Assignment Details

**Endpoint:** `GET /api/supervisors/patients/{patientId}`

**Description:** Retrieves specific patient assignment details

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Example:** `GET /api/supervisors/patients/50`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "supervisorId": 1,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "patientEmail": "alice@example.com",
    "patientPhoneNumber": "+1234567890",
    "assignmentStatus": "ACTIVE",
    "assignedAt": "2026-01-04T10:00:00",
    "assignedBy": 100,
    "assignmentNotes": "Requires regular monitoring",
    "createdAt": "2026-01-04T10:00:00",
    "updatedAt": "2026-01-04T10:00:00"
  }
}
```

---

### 6. Remove Patient Assignment

**Endpoint:** `DELETE /api/supervisors/patients/{patientId}`

**Description:** Removes patient assignment from supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Query Parameters:**
- `reason` (optional): Reason for removal

**Example:** `DELETE /api/supervisors/patients/50?reason=Patient%20transferred`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Patient assignment removed successfully",
  "data": null
}
```

---

### 7. Get Patient IDs

**Endpoint:** `GET /api/supervisors/patients/ids`

**Description:** Retrieves list of patient IDs assigned to supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [50, 51, 52, 53, 54]
}
```

---

## Case Management

### 1. Submit Case for Patient

**Endpoint:** `POST /api/supervisors/cases/patient/{patientId}`

**Description:** Submits a medical case on behalf of an assigned patient

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Example:** `POST /api/supervisors/cases/patient/50`

**Request Body:**
```json
{
  "caseTitle": "Chronic Back Pain Assessment",
  "description": "Patient experiencing severe lower back pain for the past 3 weeks. Pain intensity: 7/10. Limited mobility. No previous surgeries. Pain worsens with movement.",
  "primaryDiseaseCode": "M54.5",
  "secondaryDiseaseCodes": ["M25.5", "M79.3"],
  "symptomCodes": ["R52", "R53.83"],
  "currentMedicationCodes": ["N02BE01", "M01AE01"],
  "requiredSpecialization": "ORTHOPEDICS",
  "secondarySpecializations": ["PHYSIOTHERAPY", "PAIN_MANAGEMENT"],
  "urgencyLevel": "MEDIUM",
  "complexity": "MODERATE",
  "requiresSecondOpinion": true,
  "minDoctorsRequired": 2,
  "maxDoctorsAllowed": 3,
  "dependentId": null
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Case submitted successfully",
  "data": {
    "id": 1001,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "dependantId": null,
    "caseTitle": "Chronic Back Pain Assessment",
    "description": "Patient experiencing severe lower back pain for the past 3 weeks...",
    "status": "PENDING",
    "requiredSpecialization": "ORTHOPEDICS",
    "createdAt": "2026-01-04T10:00:00",
    "primaryDiseaseCode": "M54.5",
    "secondaryDiseaseCodes": ["M25.5", "M79.3"],
    "symptomCodes": ["R52", "R53.83"],
    "currentMedicationCodes": ["N02BE01", "M01AE01"],
    "secondarySpecializations": ["PHYSIOTHERAPY", "PAIN_MANAGEMENT"],
    "paymentStatus": "PENDING",
    "complexity": "MODERATE",
    "urgencyLevel": "MEDIUM",
    "requiresSecondOpinion": true,
    "minDoctorsRequired": 2,
    "maxDoctorsAllowed": 3,
    "submittedAt": "2026-01-04T10:00:00",
    "firstAssignedAt": null,
    "lastAssignedAt": null,
    "closedAt": null,
    "assignmentAttempts": 0,
    "rejectionCount": 0,
    "isDeleted": false,
    "consultationFee": 150.00,
    "feeSetAt": "2026-01-04T10:00:00",
    "medicalReportFileLink": null,
    "reportId": null
  }
}
```

**Error Response (403 Forbidden):**
```json
{
  "success": false,
  "message": "Patient is not assigned to this supervisor",
  "data": null
}
```

---

### 2. Get All Cases

**Endpoint:** `GET /api/supervisors/cases`

**Description:** Retrieves all cases for patients under supervisor's care

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1001,
      "patientId": 50,
      "patientName": "Alice Johnson",
      "caseTitle": "Chronic Back Pain Assessment",
      "status": "IN_PROGRESS",
      "urgencyLevel": "MEDIUM",
      "complexity": "MODERATE",
      "requiredSpecialization": "ORTHOPEDICS",
      "paymentStatus": "COMPLETED",
      "consultationFee": 150.00,
      "createdAt": "2026-01-04T10:00:00",
      "submittedAt": "2026-01-04T10:00:00"
    },
    {
      "id": 1002,
      "patientId": 51,
      "patientName": "Bob Williams",
      "caseTitle": "Diabetes Management Consultation",
      "status": "PENDING",
      "urgencyLevel": "LOW",
      "complexity": "SIMPLE",
      "requiredSpecialization": "ENDOCRINOLOGY",
      "paymentStatus": "PENDING",
      "consultationFee": 120.00,
      "createdAt": "2026-01-03T14:00:00",
      "submittedAt": "2026-01-03T14:00:00"
    }
  ]
}
```

---

### 3. Get Cases for Specific Patient

**Endpoint:** `GET /api/supervisors/cases/patient/{patientId}`

**Description:** Retrieves all cases for a specific patient

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Example:** `GET /api/supervisors/cases/patient/50`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1001,
      "patientId": 50,
      "patientName": "Alice Johnson",
      "caseTitle": "Chronic Back Pain Assessment",
      "description": "Patient experiencing severe lower back pain...",
      "status": "IN_PROGRESS",
      "urgencyLevel": "MEDIUM",
      "requiredSpecialization": "ORTHOPEDICS",
      "createdAt": "2026-01-04T10:00:00",
      "consultationFee": 150.00,
      "paymentStatus": "COMPLETED"
    }
  ]
}
```

---

### 4. Get Case Details

**Endpoint:** `GET /api/supervisors/cases/{caseId}`

**Description:** Retrieves detailed information about a specific case

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Example:** `GET /api/supervisors/cases/1001`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1001,
    "patientId": 50,
    "patientName": "Alice Johnson",
    "caseTitle": "Chronic Back Pain Assessment",
    "description": "Patient experiencing severe lower back pain for the past 3 weeks. Pain intensity: 7/10. Limited mobility.",
    "status": "IN_PROGRESS",
    "requiredSpecialization": "ORTHOPEDICS",
    "primaryDiseaseCode": "M54.5",
    "secondaryDiseaseCodes": ["M25.5", "M79.3"],
    "symptomCodes": ["R52", "R53.83"],
    "currentMedicationCodes": ["N02BE01", "M01AE01"],
    "secondarySpecializations": ["PHYSIOTHERAPY", "PAIN_MANAGEMENT"],
    "urgencyLevel": "MEDIUM",
    "complexity": "MODERATE",
    "requiresSecondOpinion": true,
    "minDoctorsRequired": 2,
    "maxDoctorsAllowed": 3,
    "paymentStatus": "COMPLETED",
    "consultationFee": 150.00,
    "createdAt": "2026-01-04T10:00:00",
    "submittedAt": "2026-01-04T10:00:00",
    "firstAssignedAt": "2026-01-04T11:00:00",
    "lastAssignedAt": "2026-01-04T11:00:00",
    "closedAt": null,
    "assignmentAttempts": 1,
    "rejectionCount": 0,
    "assignedDoctorId": 200,
    "doctorName": "Dr. Emily Roberts",
    "medicalReportFileLink": null,
    "reportId": null
  }
}
```

---

### 5. Update Case

**Endpoint:** `PUT /api/supervisors/cases/{caseId}`

**Description:** Updates case information before it's assigned to a doctor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Example:** `PUT /api/supervisors/cases/1001`

**Request Body (All fields optional):**
```json
{
  "caseTitle": "Chronic Lower Back Pain - Updated",
  "description": "Updated description with more details...",
  "urgencyLevel": "HIGH",
  "complexity": "COMPLEX"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Case updated successfully",
  "data": {
    "id": 1001,
    "caseTitle": "Chronic Lower Back Pain - Updated",
    "description": "Updated description with more details...",
    "urgencyLevel": "HIGH",
    "complexity": "COMPLEX",
    "updatedAt": "2026-01-04T12:00:00"
  }
}
```

---

### 6. Cancel Case

**Endpoint:** `PUT /api/supervisors/cases/{caseId}/cancel`

**Description:** Cancels a case before it's assigned to a doctor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Query Parameters:**
- `reason` (required): Cancellation reason

**Example:** `PUT /api/supervisors/cases/1001/cancel?reason=Patient%20no%20longer%20needs%20consultation`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Case cancelled successfully",
  "data": null
}
```

---

### 7. Get Patient Info

**Endpoint:** `GET /api/supervisors/cases/patient/{patientId}/info`

**Description:** Retrieves patient information for case submission

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Example:** `GET /api/supervisors/cases/patient/50/info`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 50,
    "userId": 200,
    "fullName": "Alice Johnson",
    "email": "alice@example.com",
    "phoneNumber": "+1234567890",
    "dateOfBirth": "1990-05-15",
    "gender": "FEMALE",
    "bloodGroup": "O_POSITIVE",
    "allergies": "Penicillin",
    "chronicConditions": "Hypertension",
    "address": "123 Oak St",
    "city": "New York",
    "country": "USA",
    "subscriptionStatus": "ACTIVE"
  }
}
```

---

## Dashboard & Analytics

### 1. Get Dashboard Statistics

**Endpoint:** `GET /api/supervisors/dashboard/statistics`

**Description:** Retrieves comprehensive dashboard statistics for supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "supervisorId": 1,
    "supervisorName": "Dr. John Smith",
    "verificationStatus": "VERIFIED",
    "activePatientCount": 15,
    "totalCasesSubmitted": 45,
    "totalCouponsIssued": 50,
    "totalCouponValue": 5000.00,
    "totalPaymentsProcessed": 35,
    "lastActivityAt": "2026-01-04T10:00:00",
    "totalPatients": 20,
    "activePatients": 15,
    "maxPatientsLimit": 20,
    "totalCases": 45,
    "activeCases": 12,
    "completedCases": 30,
    "pendingCases": 3,
    "upcomingAppointments": 5,
    "completedAppointments": 35,
    "totalAppointments": 40,
    "totalCoupons": 50,
    "availableCoupons": 20,
    "usedCoupons": 25,
    "expiredCoupons": 5,
    "availableCouponValue": 2000.00,
    "totalPayments": 35,
    "directPayments": 20,
    "couponPayments": 15,
    "totalPaymentAmount": 5250.00,
    "profileCompletionPercentage": 95,
    "missingProfileFields": ["licenseDocument"]
  }
}
```

---

### 2. Get Recent Activity

**Endpoint:** `GET /api/supervisors/dashboard/activity`

**Description:** Retrieves recent activity timeline for supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Query Parameters:**
- `limit` (optional, default: 20): Number of activities to retrieve

**Example:** `GET /api/supervisors/dashboard/activity?limit=10`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "activityType": "CASE_SUBMITTED",
      "title": "Case Submitted",
      "description": "Case submitted for patient Alice Johnson - Chronic Back Pain",
      "relatedEntityId": 1001,
      "relatedEntityType": "CASE",
      "timestamp": "2026-01-04T10:00:00"
    },
    {
      "activityType": "PATIENT_ASSIGNED",
      "title": "Patient Assigned",
      "description": "New patient assigned: Bob Williams",
      "relatedEntityId": 51,
      "relatedEntityType": "PATIENT",
      "timestamp": "2026-01-03T14:30:00"
    },
    {
      "activityType": "COUPON_REDEEMED",
      "title": "Coupon Redeemed",
      "description": "Coupon SAVE20 redeemed for case #1001",
      "relatedEntityId": 1001,
      "relatedEntityType": "COUPON",
      "timestamp": "2026-01-03T12:00:00"
    }
  ]
}
```

---

### 3. Get Performance Metrics

**Endpoint:** `GET /api/supervisors/dashboard/metrics`

**Description:** Retrieves performance metrics and analytics

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "message": "Performance metrics endpoint - to be fully implemented",
    "note": "Requires data from patient-service, doctor-service, and payment-service"
  }
}
```

---

## Payment & Coupons

### 1. Redeem Coupon for Case

**Endpoint:** `POST /api/supervisors/payments/coupon/{caseId}`

**Description:** Redeems a coupon to pay for a case consultation

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Query Parameters:**
- `patientId` (required): Patient ID

**Example:** `POST /api/supervisors/payments/coupon/1001?patientId=50`

**Request Body:**
```json
{
  "couponCode": "SAVE20"
}
```

**Success Response (200 OK):**
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
    "processedAt": "2026-01-04T10:00:00"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Coupon code is invalid or expired",
  "data": null
}
```

---

### 2. Get Coupon Summary

**Endpoint:** `GET /api/supervisors/payments/coupons`

**Description:** Retrieves coupon statistics for the supervisor

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": {
    "totalCoupons": 50,
    "availableCoupons": 20,
    "usedCoupons": 25,
    "expiredCoupons": 5,
    "totalAvailableValue": 2000.00
  }
}
```

---

### 3. Get Available Coupons for Patient

**Endpoint:** `GET /api/supervisors/payments/coupons/patient/{patientId}`

**Description:** Retrieves available coupons for a specific patient

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Example:** `GET /api/supervisors/payments/coupons/patient/50`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "couponCode": "SAVE20",
      "discountAmount": 30.00,
      "discountPercentage": 20,
      "status": "AVAILABLE",
      "issuedAt": "2026-01-01T09:00:00",
      "expiresAt": "2026-02-01T09:00:00",
      "patientId": 50,
      "supervisorId": 1
    },
    {
      "id": 2,
      "couponCode": "WELCOME50",
      "discountAmount": 75.00,
      "discountPercentage": 50,
      "status": "AVAILABLE",
      "issuedAt": "2025-12-25T09:00:00",
      "expiresAt": "2026-01-25T09:00:00",
      "patientId": 50,
      "supervisorId": 1
    }
  ]
}
```

---

### 4. Get Payment History

**Endpoint:** `GET /api/supervisors/payments/history`

**Description:** Retrieves payment history for the supervisor (Placeholder - Phase 5)

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Query Parameters:**
- `patientId` (optional): Filter by patient ID

**Example:** `GET /api/supervisors/payments/history?patientId=50`

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment history endpoint - to be implemented in Phase 5",
  "data": null
}
```

---

## Settings

### 1. Get Settings

**Endpoint:** `GET /api/supervisors/settings`

**Description:** Retrieves supervisor notification and preference settings

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": null,
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

**Endpoint:** `PUT /api/supervisors/settings`

**Description:** Updates supervisor notification and preference settings

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 100
Content-Type: application/json
```

**Request Body (All fields optional):**
```json
{
  "emailNotifications": true,
  "smsNotifications": true,
  "pushNotifications": false,
  "newCaseAssignmentNotification": true,
  "appointmentRemindersNotification": false,
  "caseStatusUpdateNotification": true,
  "couponIssuedNotification": true,
  "couponExpiringNotification": false,
  "preferredLanguage": "en",
  "timezone": "America/Los_Angeles",
  "theme": "dark"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Settings updated successfully",
  "data": {
    "id": 1,
    "supervisorId": 1,
    "emailNotifications": true,
    "smsNotifications": true,
    "pushNotifications": false,
    "theme": "dark",
    "timezone": "America/Los_Angeles"
  }
}
```

---

## Common Enums

### Verification Status
```
PENDING     - Awaiting verification
VERIFIED    - Verified and active
REJECTED    - Verification rejected
SUSPENDED   - Account suspended
```

### Case Status
```
SUBMITTED    - Case submitted
PENDING      - Awaiting assignment
ASSIGNED     - Assigned to doctor
IN_PROGRESS  - Doctor working on case
COMPLETED    - Case completed
CLOSED       - Case closed
```

### Urgency Level
```
LOW          - Low urgency
MEDIUM       - Medium urgency
HIGH         - High urgency
CRITICAL     - Critical urgency
```

### Case Complexity
```
SIMPLE           - Simple case
MODERATE         - Moderate complexity
COMPLEX          - Complex case
VERY_COMPLEX     - Very complex case
```

### Payment Status
```
PENDING      - Payment pending
COMPLETED    - Payment completed
FAILED       - Payment failed
REFUNDED     - Payment refunded
```

### Gender
```
MALE
FEMALE
OTHER
```

### Assignment Status
```
ACTIVE       - Assignment active
TERMINATED   - Assignment terminated
```

---

## Error Responses

### Common HTTP Status Codes

| Status Code | Description |
|------------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request - Validation error or invalid input |
| 401 | Unauthorized - Missing or invalid authentication |
| 403 | Forbidden - Insufficient permissions or inactive supervisor |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource conflict (e.g., duplicate assignment) |
| 500 | Internal Server Error - Server-side error |

### Error Response Format

All error responses follow this format:

```json
{
  "success": false,
  "message": "Error description here",
  "data": null
}
```

### Common Error Examples

**400 Bad Request:**
```json
{
  "success": false,
  "message": "Full name is required",
  "data": null
}
```

**401 Unauthorized:**
```json
{
  "success": false,
  "message": "Authentication required",
  "data": null
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "message": "Supervisor is not verified or inactive",
  "data": null
}
```

**404 Not Found:**
```json
{
  "success": false,
  "message": "Patient not found",
  "data": null
}
```

**409 Conflict:**
```json
{
  "success": false,
  "message": "Patient is already assigned to this supervisor",
  "data": null
}
```

---

## Quick Start Example (JavaScript/Fetch)

```javascript
// Configuration
const API_BASE_URL = 'http://localhost:8085';
const token = 'your_jwt_token_here';
const userId = '100';

// Helper function for API calls
async function apiCall(endpoint, method = 'GET', body = null) {
  const headers = {
    'Authorization': `Bearer ${token}`,
    'X-User-Id': userId,
    'Content-Type': 'application/json'
  };

  const config = { method, headers };
  if (body) config.body = JSON.stringify(body);

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
  return await response.json();
}

// Example 1: Get Dashboard Statistics
const dashboard = await apiCall('/api/supervisors/dashboard/statistics');
console.log('Dashboard:', dashboard.data);

// Example 2: Create and Assign Patient
const newPatient = await apiCall('/api/supervisors/patients/create-and-assign', 'POST', {
  fullName: "Sarah Martinez",
  email: "sarah@example.com",
  phoneNumber: "+1234567892",
  dateOfBirth: "1985-05-15",
  gender: "FEMALE",
  address: "789 Pine St",
  city: "Los Angeles",
  country: "USA",
  emergencyContactName: "Carlos Martinez",
  emergencyContactPhone: "+1234567893"
});
console.log('New Patient:', newPatient.data);

// Example 3: Submit Case
const newCase = await apiCall('/api/supervisors/cases/patient/50', 'POST', {
  caseTitle: "Annual Health Checkup",
  description: "Routine annual health checkup for preventive care...",
  requiredSpecialization: "GENERAL_MEDICINE",
  urgencyLevel: "LOW",
  complexity: "SIMPLE",
  requiresSecondOpinion: false,
  minDoctorsRequired: 1,
  maxDoctorsAllowed: 1
});
console.log('Case Submitted:', newCase.data);

// Example 4: Get All Patients
const patients = await apiCall('/api/supervisors/patients');
console.log('Patients:', patients.data);

// Example 5: Redeem Coupon
const payment = await apiCall(
  '/api/supervisors/payments/coupon/1001?patientId=50',
  'POST',
  { couponCode: 'SAVE20' }
);
console.log('Payment Result:', payment.data);
```

---

## Quick Start Example (Axios)

```javascript
import axios from 'axios';

// Create axios instance
const api = axios.create({
  baseURL: 'http://localhost:8085',
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-User-Id': userId
  }
});

// Get Dashboard
const dashboard = await api.get('/api/supervisors/dashboard/statistics');

// Create Patient
const patient = await api.post('/api/supervisors/patients/create-and-assign', {
  fullName: "Sarah Martinez",
  email: "sarah@example.com",
  phoneNumber: "+1234567892",
  dateOfBirth: "1985-05-15",
  gender: "FEMALE"
});

// Submit Case
const caseData = await api.post('/api/supervisors/cases/patient/50', {
  caseTitle: "Annual Checkup",
  description: "Routine checkup",
  requiredSpecialization: "GENERAL_MEDICINE",
  urgencyLevel: "LOW"
});
```

---

**For questions or support, contact the backend development team.**

**Last Updated:** January 4, 2026