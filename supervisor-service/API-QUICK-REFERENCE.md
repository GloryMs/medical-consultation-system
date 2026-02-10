# Supervisor API Quick Reference

**Base URL:** `http://localhost:8085`
**Auth Required:** All endpoints require `Authorization: Bearer <token>` and `X-User-Id: <userId>`

---

## 👤 Profile

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/supervisors/profile` | Create profile |
| GET | `/api/supervisors/profile` | Get profile |
| PUT | `/api/supervisors/profile` | Update profile |
| POST | `/api/supervisors/profile/license-document` | Upload license (multipart) |
| DELETE | `/api/supervisors/profile` | Delete profile |

---

## 👥 Patients

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/supervisors/patients` | Get all assigned patients |
| POST | `/api/supervisors/patients?patientId={id}&notes={notes}` | Assign patient by ID |
| POST | `/api/supervisors/patients/create-and-assign` | Create new patient and assign |
| POST | `/api/supervisors/patients/assign-by-email` | Assign existing patient by email |
| GET | `/api/supervisors/patients/{patientId}` | Get patient assignment |
| DELETE | `/api/supervisors/patients/{patientId}?reason={reason}` | Remove assignment |
| GET | `/api/supervisors/patients/ids` | Get patient IDs list |

---

## 📋 Cases

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/supervisors/cases/patient/{patientId}` | Submit case for patient |
| GET | `/api/supervisors/cases` | Get all cases |
| GET | `/api/supervisors/cases/patient/{patientId}` | Get patient cases |
| GET | `/api/supervisors/cases/{caseId}` | Get case details |
| PUT | `/api/supervisors/cases/{caseId}` | Update case |
| PUT | `/api/supervisors/cases/{caseId}/cancel?reason={reason}` | Cancel case |
| GET | `/api/supervisors/cases/patient/{patientId}/info` | Get patient info |

---

## 📊 Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/supervisors/dashboard/statistics` | Get dashboard stats |
| GET | `/api/supervisors/dashboard/activity?limit={n}` | Get recent activity |
| GET | `/api/supervisors/dashboard/metrics` | Get performance metrics |

---

## 💰 Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/supervisors/payments/coupon/{caseId}?patientId={id}` | Redeem coupon |
| GET | `/api/supervisors/payments/coupons` | Get coupon summary |
| GET | `/api/supervisors/payments/coupons/patient/{patientId}` | Get available coupons |
| GET | `/api/supervisors/payments/history?patientId={id}` | Get payment history |

---

## ⚙️ Settings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/supervisors/settings` | Get settings |
| PUT | `/api/supervisors/settings` | Update settings |

---

## 📝 Request Body Fields

### Create Patient and Assign (`/patients/create-and-assign`)
```json
{
  "email": "patient@example.com",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "bloodType": "O_POSITIVE"
}
```

### Assign Patient by Email (`/patients/assign-by-email`)
```json
{
  "patientEmail": "patient@example.com",
  "notes": "Optional assignment notes"
}
```

### Case Submission Fields (`/cases/patient/{patientId}`)

**Required:**
- `caseTitle` (string, 10-100 chars)
- `description` (string, 50-2000 chars)
- `requiredSpecialization` (string)
- `urgencyLevel` (enum: LOW, MEDIUM, HIGH, CRITICAL)

**Optional:**
- `primaryDiseaseCode` (string)
- `secondaryDiseaseCodes` (array)
- `symptomCodes` (array)
- `currentMedicationCodes` (array)
- `secondarySpecializations` (array)
- `complexity` (enum: SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX)
- `requiresSecondOpinion` (boolean, default: true)
- `minDoctorsRequired` (integer, default: 2)
- `maxDoctorsAllowed` (integer, default: 3)
- `dependentId` (number, optional)

---

## 🎨 Common Enums

**VerificationStatus:** `PENDING` | `VERIFIED` | `REJECTED` | `SUSPENDED`

**CaseStatus:** `SUBMITTED` | `PENDING` | `ASSIGNED` | `IN_PROGRESS` | `COMPLETED` | `CLOSED`

**UrgencyLevel:** `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`

**CaseComplexity:** `SIMPLE` | `MODERATE` | `COMPLEX` | `VERY_COMPLEX`

**PaymentStatus:** `PENDING` | `COMPLETED` | `FAILED` | `REFUNDED`

---

## ⚡ Response Format

```json
{
  "success": true,
  "message": "Optional message",
  "data": { /* Response data */ }
}
```

---

## ❌ HTTP Status Codes

- **200** - Success
- **201** - Created
- **400** - Bad Request
- **401** - Unauthorized
- **403** - Forbidden
- **404** - Not Found
- **409** - Conflict
- **500** - Server Error

---

**Full Documentation:** See `SUPERVISOR-API-DOCUMENTATION.md`