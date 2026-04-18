# ALIS — Backend API
### Client Authentication & Audit System

> **Legal AI platform** built for POPIA & CPA compliance in South Africa.
> Role-based access control for Deal Makers, Legal Practitioners, and Admins.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-111111?style=for-the-badge)

---

## Table of Contents

- [Overview](#overview)
- [Base URL](#base-url)
- [Client Auth API](#-client-auth-api)
  - [Register Client](#1-register-client)
  - [Login Client](#2-login-client)
- [Audit Log API](#-audit-log-api--admin-only)
  - [Get Recent Logs](#1-get-recent-logs)
  - [Get All Logs](#2-get-all-logs)
  - [Get Logs by Client](#3-get-logs-by-client)
- [System Behaviour](#-system-behaviour)
- [Role Permissions](#-role-permissions)
- [Tech Stack](#-tech-stack)

---

## Overview

This API provides the core authentication and audit infrastructure for the **ALIS Legal AI** platform.

- ✅ Client registration with automatic audit log creation
- ✅ Client login with session tracking
- ✅ Admin-only audit log viewing *(read-only)*
- ✅ Full POPIA & CPA compliance audit trail

---

## Base URL

```
http://localhost:8080
```

> Replace with your Render deployment URL when going to production.

---

## 🔐 Client Auth API

### 1. Register Client

Registers a new client and automatically creates an audit log entry with action type `USER_CREATED`.

**Endpoint**

```
POST /api/auth/register
```

**Request Body**

```json
{
  "username":     "owen",
  "fullName":     "Owen Nino",
  "email":        "owen@gmail.com",
  "passwordHash": "12345",
  "role":         "DEAL_MAKER"
}
```

**Response** `200 OK`

```json
{
  "message":  "Registration successful",
  "clientId": 3,
  "email":    "owen@gmail.com",
  "role":     "DEAL_MAKER"
}
```

---

### 2. Login Client

Authenticates a client and writes a `LOGIN` action to the audit log.

**Endpoint**

```
POST /api/auth/login
```

**Request Body**

```json
{
  "email":    "owen@gmail.com",
  "password": "12345"
}
```

**Response** `200 OK`

```json
{
  "message":  "Login successful",
  "clientId": 3,
  "email":    "owen@gmail.com",
  "role":     "DEAL_MAKER",
  "fullName": "Owen"
}
```

---

## 📜 Audit Log API — Admin Only

> All audit endpoints are restricted to users with the `ADMIN` role.
> No user — including Legal Practitioners — can delete audit log entries.

### 1. Get Recent Logs

Returns the latest **20** system actions ordered by most recent first.

**Endpoint**

```
GET /api/admin/audit/recent
```

**Response** `200 OK`

```json
[
  {
    "logId":       4,
    "actionType":  "LOGIN",
    "description": "Successful login: owen@gmail.com",
    "ipAddress":   "SYSTEM",
    "createdAt":   "2026-04-18T20:33:23.650",
    "clientId":    3,
    "adminId":     null,
    "documentId":  null
  }
]
```

---

### 2. Get All Logs

Returns **all** audit log records ordered by newest first. Use for full compliance audits.

**Endpoint**

```
GET /api/admin/audit
```

---

### 3. Get Logs by Client

Filters audit logs for a specific client. Useful for investigating a single user's activity.

**Endpoint**

```
GET /api/admin/audit/client/{clientId}
```

**Example**

```
GET /api/admin/audit/client/3
```

---

## 🧠 System Behaviour

Every user action that modifies state automatically generates an audit log entry, ensuring full traceability for POPIA compliance and security monitoring.

| Action Type       | Trigger                                      |
|-------------------|----------------------------------------------|
| `USER_CREATED`    | Client completes registration                |
| `LOGIN`           | Client authenticates successfully            |
| `LOGOUT`          | Client ends session                          |
| `UPLOAD_DOCUMENT` | Deal Maker uploads a contract                |
| `ANALYSIS_RUN`    | AI compliance engine processes a document    |
| `COMPLIANCE_CHECK`| Manual compliance check triggered            |
| `REPORT_GENERATED`| Summary report created                       |
| `USER_UPDATED`    | Admin modifies user details                  |
| `USER_DELETED`    | Admin removes a user account                 |
| `SYSTEM_ERROR`    | Unhandled exception caught by the system     |

---

## 👥 Role Permissions

| Role                   | Key Capabilities                                                              |
|------------------------|-------------------------------------------------------------------------------|
| `DEAL_MAKER`           | Upload documents, request compliance check, view AI results                   |
| `LEGAL_PRACTITIONER`   | Manage laws / acts / rules, review contracts, override compliance results     |
| `ADMIN`                | Manage users, assign roles, view audit logs, system monitoring                |

### Role boundary rules

```
DEAL_MAKER           →  submit & consume legal feedback
LEGAL_PRACTITIONER   →  legal authority + rule manager + reviewer
ADMIN                →  platform controller, not a legal actor
```

---

## 🛠 Tech Stack

| Component    | Technology                              |
|--------------|-----------------------------------------|
| Framework    | Spring Boot 3.x                         |
| Database     | PostgreSQL 15                           |
| ORM          | Spring Data JPA / Hibernate             |
| Auth         | Password hash (BCrypt)                  |
| Deployment   | Render / localhost:8080                 |
| Compliance   | POPIA, CPA — South Africa               |

---

```

---

## ⚙️ Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 15
- Maven 3.8+

### 1. Clone the repository

```bash
git clone https://github.com/your-username/alis-backend.git
cd alis-backend
```

### 2. Configure the database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/alis_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## 🔒 Security Notes

- Passwords should be hashed using **BCrypt** before storage — never store plain text
- All audit logs are **immutable** — no endpoint allows deletion or modification
- Admin endpoints should be protected with role-based middleware in production

---

*ALIS Legal AI System — Built for South African legal compliance*
