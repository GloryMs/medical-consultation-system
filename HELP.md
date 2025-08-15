// ==========================================
// 13. README FILE
// ==========================================

// README.md
# Medical Consultation System - Microservices Backend

## Architecture Overview
This is a microservices-based medical consultation system built with Spring Boot.

### Services:
1. **Eureka Server** (Port 8761) - Service Discovery
2. **API Gateway** (Port 8080) - Entry point for all requests
3. **Auth Service** (Port 8081) - Authentication & Authorization
4. **Patient Service** (Port 8082) - Patient management
5. **Doctor Service** (Port 8083) - Doctor management
6. **Admin Service** (Port 8084) - Administrative functions
7. **Payment Service** (Port 8085) - Payment processing
8. **Integration Service** (Port 8086) - Third-party integrations

## Prerequisites
- Java 17
- Maven 3.8+
- PostgreSQL 15
- Docker & Docker Compose (optional)

## Setup Instructions

### 1. Database Setup
```bash
# Create databases
CREATE DATABASE medical_auth_db;
CREATE DATABASE medical_patient_db;
CREATE DATABASE medical_doctor_db;
CREATE DATABASE medical_admin_db;
CREATE DATABASE medical_payment_db;
```

### 2. Build All Services
```bash
mvn clean install
```

### 3. Start Services (in order)
```bash
# 1. Start Eureka Server
cd eureka-server && mvn spring-boot:run

# 2. Start API Gateway
cd api-gateway && mvn spring-boot:run

# 3. Start Auth Service
cd auth-service && mvn spring-boot:run

# 4. Start Patient Service
cd patient-service && mvn spring-boot:run

# 5. Start Doctor Service
cd doctor-service && mvn spring-boot:run

# 6. Start Admin Service
cd admin-service && mvn spring-boot:run

# 7. Start Payment Service
cd payment-service && mvn spring-boot:run

# 8. Start Integration Service
cd integration-service && mvn spring-boot:run
```

### Or use Docker Compose:
```bash
docker-compose up
```

## API Endpoints

### Authentication
- POST `/api/auth/register` - Register new user
- POST `/api/auth/login` - Login
- POST `/api/auth/google` - Google OAuth login

### Patient APIs
- POST `/api/patients/profile` - Create patient profile
- GET `/api/patients/profile` - Get patient profile
- POST `/api/patients/subscription` - Create subscription
- POST `/api/patients/cases` - Submit new case
- GET `/api/patients/cases` - Get patient cases

### Doctor APIs
- POST `/api/doctors/profile` - Create doctor profile
- POST `/api/doctors/cases/{caseId}/accept` - Accept case
- POST `/api/doctors/appointments` - Schedule appointment
- GET `/api/doctors/appointments` - Get appointments
- POST `/api/doctors/consultation-reports` - Create consultation report

### Admin APIs
- GET `/api/admin/dashboard` - Get dashboard stats
- POST `/api/admin/doctors/verify` - Verify doctor

### Payment APIs
- POST `/api/payments/process` - Process payment

## Testing

### Test User Registration
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@test.com",
    "password": "Test@1234",
    "role": "PATIENT",
    "fullName": "John Doe"
  }'
```

### Test Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@test.com",
    "password": "Test@1234"
  }'
```

## Business Rules Implemented
1. Patients must have active subscription to submit cases
2. Case status workflow: SUBMITTED → PENDING → ACCEPTED → SCHEDULED → PAYMENT_PENDING → IN_PROGRESS
3. Cases only move to IN_PROGRESS after payment completion
4. Doctors must be verified before accepting cases

## Security
- JWT-based authentication
- Role-based access control (ADMIN, DOCTOR, PATIENT)
- All endpoints except auth are protected

## Notes
- Third-party integrations (Zoom, WhatsApp, PayPal) are simulated
- Payment processing is simulated
- Google OAuth uses mock implementation for testing