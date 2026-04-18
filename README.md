# ZBankCard — Credit Card Application System

A Spring Boot REST API for end-to-end credit card application management — from customer registration and credit scoring to card issuance, PIN management, and account login.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security + JWT (stateless) |
| Database | MySQL 8+ |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Password Hashing | BCrypt |

---

## Prerequisites

- Java 17+
- MySQL 8+ running locally
- Maven 3.6+ (or use the included `mvnw`)

---

## Database Setup

Create the schema before starting the application:

```sql
CREATE DATABASE carddb;
```

The application uses `spring.jpa.hibernate.ddl-auto=update` — all tables are auto-created on first startup.

**`application.properties` configuration:**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/carddb
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=mySuperSecretKeyThatIsAtLeast32BytesLong12345
```

---

## Running the Application

```bash
./mvnw spring-boot:run
```

Server starts on **`http://localhost:8080`**

---

## Database Schema

### `customers`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| name | VARCHAR(150) | |
| email | VARCHAR(255) | Unique |
| phone | VARCHAR(15) | Unique |
| dob | DATE | |
| address | VARCHAR(500) | |
| password_hash | VARCHAR(255) | BCrypt hashed |
| created_at | DATETIME | |
| updated_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

### `credit_card_applications`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| customer_id | UUID | FK → customers |
| application_number | VARCHAR | e.g. `ZBNK-20260418-001234` |
| status | ENUM | `PENDING`, `APPROVED`, `UNDER_REVIEW`, `REJECTED`, `CANCELLED` |
| applied_at | DATETIME | |
| created_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

### `credit_scores`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| customer_id | UUID | FK → customers |
| application_id | UUID | FK → credit_card_applications |
| score | INT | 50 / 150 / 300 / 500 |
| score_source | VARCHAR | `Z-Bank Internal (Calculated)` or `Z-Bank Internal (Historical Score)` |
| card_count | INT | Existing cards at evaluation time |
| created_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

### `employments`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| customer_id | UUID | FK → customers |
| employer_name | VARCHAR(200) | |
| employment_type | ENUM | `SALARIED`, `SELF_EMPLOYED`, `BUSINESS`, `STUDENT`, `RETIRED`, `UNEMPLOYED` |
| salary | DECIMAL(15,2) | Monthly gross salary |
| designation | VARCHAR(150) | |
| created_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

### `documents`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| customer_id | UUID | FK → customers |
| doc_type | ENUM | `AADHAAR`, `PAN`, `PASSPORT`, `DRIVING_LICENSE`, `VOTER_ID`, `UTILITY_BILL` |
| doc_number | VARCHAR(100) | Stored uppercase |
| expiry_date | DATE | Optional |
| created_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

### `credit_cards`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| customer_id | UUID | FK → customers |
| application_id | UUID | FK → credit_card_applications (unique) |
| card_number | VARCHAR(255) | 16-digit, Luhn-valid |
| card_type | ENUM | `PLATINUM`, `GOLD`, `VISA` |
| credit_limit | DECIMAL(15,2) | |
| status | ENUM | `ACTIVE`, `INACTIVE`, `BLOCKED`, `EXPIRED`, `CANCELLED` |
| issued_at | DATETIME | |
| created_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

### `card_pins`
| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| card_id | UUID | FK → credit_cards (unique) |
| pin_hash | VARCHAR(255) | BCrypt hashed |
| first_time_login | BOOLEAN | `true` until customer sets own PIN |
| last_login | DATETIME | |
| failed_attempt_count | INT | Locks at 3 failures |
| last_failed_attempt_at | DATETIME | |
| created_at | DATETIME | |
| deleted | BOOLEAN | Soft delete |

---

## Credit Scoring Rules

Credit score is calculated from salary (or taken from historical record if one exists):

| Condition | Score | Card Issued |
|---|---|---|
| Existing cards ≥ 2 | 300 | GOLD |
| Monthly salary > ₹2,00,000 | 500 | PLATINUM |
| ₹50,000 < salary ≤ ₹2,00,000 | 150 | VISA |
| Salary ≤ ₹50,000 | 50 | None (UNDER_REVIEW) |

## Card Tier Allocation

| Score | Card Type | Credit Limit | Card Number Prefix |
|---|---|---|---|
| ≥ 500 | PLATINUM | ₹40,000 | 5100 |
| ≥ 300 | GOLD | ₹20,000 | 4532 |
| ≥ 150 | VISA | ₹10,000 | 4111 |
| < 150 | None | — | — |

---

## API Reference

Base URL: `http://localhost:8080`

### Authentication

Public endpoints (no token required):
- `POST /api/v1/applications`
- `GET /api/v1/applications/{applicationNumber}`
- `POST /api/v1/auth/login`
- `POST /api/v1/cards/login`
- `POST /api/v1/cards/set-pin`
- `POST /api/v1/cards/change-pin`

Protected endpoints (require `Authorization: Bearer <token>`):
- `GET /api/v1/customers/me/applications`
- `GET /api/v1/customers/me/cards`

---

### 1. Apply for Credit Card

**`POST /api/v1/applications`**

Submits a full credit card application. Automatically evaluates credit score, allocates card tier, and returns the first-time issuance PIN if approved.

**Request Body:**
```json
{
  "customerInfo": {
    "name": "Priya Sharma",
    "email": "priya.sharma@example.com",
    "phone": "9876543210",
    "dob": "1990-05-15",
    "address": "123 MG Road, Bangalore, Karnataka 560001",
    "password": "SecurePass@123"
  },
  "employmentInfo": {
    "employerName": "Infosys Limited",
    "employmentType": "SALARIED",
    "salary": 250000.00,
    "designation": "Senior Software Engineer"
  },
  "documentInfo": {
    "docType": "PAN",
    "docNumber": "ABCDE1234F",
    "expiryDate": null
  }
}
```

**Field Validations:**

| Field | Rules |
|---|---|
| `customerInfo.name` | 2–150 chars, letters/spaces/dots/hyphens only |
| `customerInfo.email` | Valid email format, max 255 chars |
| `customerInfo.phone` | 10-digit Indian mobile number (starts with 6–9) |
| `customerInfo.dob` | Must be a past date |
| `customerInfo.address` | 10–500 chars |
| `customerInfo.password` | 8–100 chars |
| `employmentInfo.employerName` | Max 200 chars |
| `employmentInfo.employmentType` | One of: `SALARIED`, `SELF_EMPLOYED`, `BUSINESS`, `STUDENT`, `RETIRED`, `UNEMPLOYED` |
| `employmentInfo.salary` | Positive decimal |
| `employmentInfo.designation` | Max 150 chars |
| `documentInfo.docType` | One of: `AADHAAR`, `PAN`, `PASSPORT`, `DRIVING_LICENSE`, `VOTER_ID`, `UTILITY_BILL` |
| `documentInfo.docNumber` | 3–100 chars |

**Responses:**

| HTTP | Condition |
|---|---|
| 201 Created | Application processed (status: APPROVED or UNDER_REVIEW) |
| 400 Bad Request | Validation failure |
| 409 Conflict | Email or phone already registered |

**Response (APPROVED — with card):**
```json
{
  "applicationId": "uuid",
  "applicationNumber": "ZBNK-20260418-001234",
  "status": "APPROVED",
  "appliedAt": "2026-04-18T10:00:00",
  "additionalDocumentsRequired": false,
  "customer": {
    "customerId": "uuid",
    "name": "Priya Sharma",
    "email": "priya.sharma@example.com",
    "phone": "9876543210"
  },
  "creditRating": {
    "score": 500,
    "scoreSource": "Z-Bank Internal (Calculated)",
    "message": "Congratulations! Your credit card application has been approved."
  },
  "cardDetails": {
    "maskedCardNumber": "**** **** **** 1234",
    "cardType": "PLATINUM",
    "creditLimit": 40000.00,
    "cardStatus": "INACTIVE",
    "issuedAt": "2026-04-18T10:00:01",
    "firstTimePin": "7391"
  }
}
```

> **Important:** Save `cardDetails.firstTimePin` — it is only shown once and is needed for the first card login.

**Response (UNDER_REVIEW — no card issued):**
```json
{
  "applicationId": "uuid",
  "applicationNumber": "ZBNK-20260418-005678",
  "status": "UNDER_REVIEW",
  "additionalDocumentsRequired": true,
  "creditRating": {
    "score": 50,
    "message": "Additional documents are required. Our team will contact you within 2-3 business days."
  }
}
```

---

### 2. Check Application Status

**`GET /api/v1/applications/{applicationNumber}`**

Returns the current status of an application including credit score and card details if approved.

**Example:** `GET /api/v1/applications/ZBNK-20260418-001234`

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | Status retrieved |
| 404 Not Found | Application number does not exist |

**Response:**
```json
{
  "applicationId": "uuid",
  "applicationNumber": "ZBNK-20260418-001234",
  "status": "APPROVED",
  "appliedAt": "2026-04-18T10:00:00",
  "additionalDocumentsRequired": false,
  "creditScore": 500,
  "cardDetails": {
    "maskedCardNumber": "**** **** **** 1234",
    "cardType": "PLATINUM",
    "creditLimit": 40000.00,
    "cardStatus": "INACTIVE",
    "issuedAt": "2026-04-18T10:00:01"
  }
}
```

---

### 3. Login (Email + Password)

**`POST /api/v1/auth/login`**

Authenticates the customer using the email and password provided at registration. Returns a JWT token along with all their applications and cards.

**Request Body:**
```json
{
  "email": "priya.sharma@example.com",
  "password": "SecurePass@123"
}
```

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | Login successful |
| 422 Unprocessable Entity | Invalid email or password |

**Response:**
```json
{
  "status": 200,
  "result": "SUCCESS",
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "firstLogin": true,
    "customer": {
      "customerId": "uuid",
      "name": "Priya Sharma",
      "email": "priya.sharma@example.com",
      "phone": "9876543210"
    },
    "applications": [
      {
        "applicationId": "uuid",
        "applicationNumber": "ZBNK-20260418-001234",
        "status": "APPROVED",
        "appliedAt": "2026-04-18T10:00:00"
      }
    ],
    "cards": [
      {
        "cardId": "uuid",
        "maskedCardNumber": "**** **** **** 1234",
        "cardType": "PLATINUM",
        "creditLimit": 40000.00,
        "cardStatus": "INACTIVE",
        "issuedAt": "2026-04-18T10:00:01"
      }
    ]
  }
}
```

> **Note:** `firstLogin: true` means at least one of your cards still has the issuance PIN set. Use the Set PIN API to update it.

---

### 4. Card Login (Card Number + PIN)

**`POST /api/v1/cards/login`**

Authenticates using the 16-digit card number and 4-digit PIN.

**Request Body:**
```json
{
  "cardNumber": "5100123456789012",
  "pin": "7391"
}
```

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | Login successful |
| 401 Unauthorized | Wrong PIN or card locked (3 failed attempts) |
| 404 Not Found | Card number not found |

**Response:**
```json
{
  "customerId": "uuid",
  "customerName": "Priya Sharma",
  "customerEmail": "priya.sharma@example.com",
  "maskedCardNumber": "**** **** **** 1234",
  "cardType": "PLATINUM",
  "creditLimit": 40000.00,
  "cardStatus": "INACTIVE",
  "firstTimeLogin": true,
  "lastLogin": null,
  "message": "Login successful. Please set your new PIN using the /api/v1/cards/set-pin endpoint."
}
```

---

### 5. Set PIN (First Time Only)

**`POST /api/v1/cards/set-pin`**

Sets a new PIN for the first time after card issuance. The `currentPin` must be the issuance PIN received in the apply response. This endpoint can only be used once — use Change PIN after the first time.

**Request Body:**
```json
{
  "cardNumber": "5100123456789012",
  "currentPin": "7391",
  "newPin": "4567"
}
```

**Field Validations:**

| Field | Rules |
|---|---|
| `cardNumber` | Exactly 16 digits |
| `currentPin` | Exactly 4 digits |
| `newPin` | Exactly 4 digits, must differ from `currentPin` |

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | PIN set successfully |
| 400 Bad Request | New PIN same as current, or PIN already set before |
| 401 Unauthorized | Wrong current PIN or card locked |

**Response:**
```json
{
  "message": "PIN set successfully. You can now log in with your new PIN."
}
```

---

### 6. Change PIN

**`POST /api/v1/cards/change-pin`**

Changes the PIN on an active card. Verifies the current PIN first.

**Request Body:**
```json
{
  "cardNumber": "5100123456789012",
  "currentPin": "4567",
  "newPin": "8901"
}
```

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | PIN changed successfully |
| 400 Bad Request | New PIN same as current PIN |
| 401 Unauthorized | Wrong current PIN or card locked |

**Response:**
```json
{
  "message": "PIN changed successfully."
}
```

---

### 7. My Applications

**`GET /api/v1/customers/me/applications`**

Returns all credit card applications submitted by the authenticated customer.

**Headers:** `Authorization: Bearer <token>`

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | Applications retrieved |
| 401 Unauthorized | Missing or invalid token |

**Response:**
```json
{
  "status": 200,
  "result": "SUCCESS",
  "message": "Applications retrieved successfully",
  "data": [
    {
      "applicationId": "uuid",
      "applicationNumber": "ZBNK-20260418-001234",
      "status": "APPROVED",
      "appliedAt": "2026-04-18T10:00:00"
    }
  ]
}
```

---

### 8. My Cards

**`GET /api/v1/customers/me/cards`**

Returns all credit cards issued to the authenticated customer.

**Headers:** `Authorization: Bearer <token>`

**Responses:**

| HTTP | Condition |
|---|---|
| 200 OK | Cards retrieved |
| 401 Unauthorized | Missing or invalid token |

**Response:**
```json
{
  "status": 200,
  "result": "SUCCESS",
  "message": "Cards retrieved successfully",
  "data": [
    {
      "cardId": "uuid",
      "maskedCardNumber": "**** **** **** 1234",
      "cardType": "PLATINUM",
      "creditLimit": 40000.00,
      "cardStatus": "INACTIVE",
      "issuedAt": "2026-04-18T10:00:01"
    }
  ]
}
```

---

## Error Response Format

All error responses follow this structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Application not found: ZBNK-20260418-999999",
  "path": "/api/v1/applications/ZBNK-20260418-999999",
  "fieldErrors": null
}
```

For validation errors (HTTP 400):
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/v1/applications",
  "fieldErrors": [
    {
      "field": "customerInfo.email",
      "rejectedValue": "not-an-email",
      "message": "Email must be a valid email address"
    }
  ]
}
```

| HTTP Status | Meaning |
|---|---|
| 400 | Validation failure |
| 401 | Invalid credentials or missing/expired JWT |
| 404 | Resource not found |
| 409 | Duplicate email or phone |
| 422 | Business rule violation |
| 500 | Unexpected server error |

---

## End-to-End Testing Flow

Follow this sequence to test all APIs:

**Step 1 — Apply for a card**
```
POST /api/v1/applications
```
Save from response: `applicationNumber`, `cardDetails.firstTimePin`, `cardDetails.maskedCardNumber` (last 4 digits)

**Step 2 — Check application status**
```
GET /api/v1/applications/{applicationNumber}
```

**Step 3 — Login with email + password**
```
POST /api/v1/auth/login
```
Save: `data.token` for use in Steps 7–8

**Step 4 — Card login (first time)**
```
POST /api/v1/cards/login
{ "cardNumber": "<16-digit>", "pin": "<firstTimePin>" }
```

**Step 5 — Set new PIN**
```
POST /api/v1/cards/set-pin
{ "cardNumber": "<16-digit>", "currentPin": "<firstTimePin>", "newPin": "4567" }
```

**Step 6 — Change PIN**
```
POST /api/v1/cards/change-pin
{ "cardNumber": "<16-digit>", "currentPin": "4567", "newPin": "8901" }
```

**Step 7 — View my applications (authenticated)**
```
GET /api/v1/customers/me/applications
Authorization: Bearer <token>
```

**Step 8 — View my cards (authenticated)**
```
GET /api/v1/customers/me/cards
Authorization: Bearer <token>
```

---

## Sample Test Data (4 Scoring Scenarios)

### PLATINUM (score 500 — salary > ₹2,00,000)
```json
{
  "customerInfo": { "name": "Priya Sharma", "email": "priya.sharma@example.com", "phone": "9876543210", "dob": "1990-05-15", "address": "123 MG Road, Bangalore, Karnataka 560001", "password": "SecurePass@123" },
  "employmentInfo": { "employerName": "Infosys Limited", "employmentType": "SALARIED", "salary": 250000.00, "designation": "Senior Software Engineer" },
  "documentInfo": { "docType": "PAN", "docNumber": "ABCDE1234F", "expiryDate": null }
}
```

### GOLD (score 150 — salary between ₹50,000–₹2,00,000, and existing cards ≥ 2 would give 300)
```json
{
  "customerInfo": { "name": "Rahul Verma", "email": "rahul.verma@example.com", "phone": "8765432109", "dob": "1988-11-20", "address": "456 Andheri West, Mumbai, Maharashtra 400053", "password": "RahulPass@456" },
  "employmentInfo": { "employerName": "Tata Consultancy Services", "employmentType": "SALARIED", "salary": 80000.00, "designation": "Business Analyst" },
  "documentInfo": { "docType": "AADHAAR", "docNumber": "123456789012", "expiryDate": null }
}
```

### VISA (score 150 — salary between ₹50,001–₹2,00,000)
```json
{
  "customerInfo": { "name": "Anjali Mehta", "email": "anjali.mehta@example.com", "phone": "7654321098", "dob": "1995-03-08", "address": "789 Connaught Place, New Delhi 110001", "password": "AnjaliPass@789" },
  "employmentInfo": { "employerName": "Local Retail Store", "employmentType": "SELF_EMPLOYED", "salary": 60000.00, "designation": "Shop Owner" },
  "documentInfo": { "docType": "DRIVING_LICENSE", "docNumber": "DL1420110012345", "expiryDate": "2030-12-31" }
}
```

### UNDER_REVIEW (score 50 — salary ≤ ₹50,000)
```json
{
  "customerInfo": { "name": "Suresh Kumar", "email": "suresh.kumar@example.com", "phone": "6543210987", "dob": "2000-07-25", "address": "101 Jayanagar, Bangalore, Karnataka 560011", "password": "SureshPass@101" },
  "employmentInfo": { "employerName": "Freelance", "employmentType": "UNEMPLOYED", "salary": 5000.00, "designation": "Freelancer" },
  "documentInfo": { "docType": "VOTER_ID", "docNumber": "ABC1234567", "expiryDate": null }
}
```

---

## Security

- Passwords are hashed with **BCrypt** before storage — never stored in plain text
- Card PINs are hashed with **BCrypt** — issuance PIN is shown only once in the apply response
- JWT tokens expire after **1 hour**
- Cards are **locked after 3 consecutive failed PIN attempts**
- All queries filter by `deleted = false` (soft delete) — no hard deletes
- All queries are user-scoped — customers cannot access other customers' data
