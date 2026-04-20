# ALIS — Legal Compliance API

> Backend API for the Automated Legal Intelligence System  
> Built for POPIA & CPA compliance in South Africa

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)

---

## 🌐 Base URL

| Environment | URL |
|-------------|-----|
| Local | `http://localhost:8081` |
| Production | `https://alis-backend.onrender.com` *(update with your actual Render URL)* |

All endpoints are prefixed with `/api`.

---

## ✅ Production‑Ready Features

| Feature | Status |
|---------|--------|
| User registration & login | ✅ Fully working |
| Admin client management (CRUD) | ✅ Fully working |
| Admin dashboard statistics | ✅ Fully working |
| Audit logging (read‑only for admins) | ✅ Fully working |
| Real‑time audit via WebSocket | ✅ Fully working |
| Document upload & AI analysis | 🚧 Under development |
| Compliance report generation | 🚧 Under development |

---

## 🔐 Authentication

### 1. Register a New Client

Creates a new user account. Role defaults to `USER` if not specified.

**Endpoint**
POST /api/auth/register

**Request Body** (JSON)

```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}

Response 200 OK

json
{
  "message": "Registration successful",
  "clientId": 1,
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "success": true
}

2. Login
Authenticates a user and returns their profile.

POST /api/auth/login

{
  "email": "john@example.com",
  "password": "securePassword123"
}
Response 200 OK

json
{
  "message": "Login successful",
  "clientId": 1,
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "success": true
}

Error Response 401 Unauthorized

json
{
  "message": "Invalid credentials",
  "success": false
}
👥 Client Management (Admin Only)
All endpoints under /api/admin/clients require ADMIN role (role enforcement not yet implemented – all requests currently permitted).

List All Clients (Paginated)
text
GET /api/admin/clients?page=0&size=5
Query Parameters

Param	Default	Description
page	0	Page number (zero‑indexed)
size	20	Items per page
sort	createdAt	Sort field
dir	desc	Sort direction (asc or desc)
Response (Page object)

json
{
  "content": [
    {
      "clientId": 1,
      "fullName": "John Doe",
      "email": "john@example.com",
      "username": "johndoe_123",
      "role": "USER",
      "createdAt": "2026-04-20T10:30:00"
    }
  ],
  "pageable": { ... },
  "totalPages": 1,
  "totalElements": 1,
  "last": true,
  "first": true,
  "empty": false
}
Get Client by ID
text
GET /api/admin/clients/{id}
Response

json
{
  "clientId": 1,
  "fullName": "John Doe",
  "email": "john@example.com",
  "username": "johndoe_123",
  "role": "USER",
  "createdAt": "2026-04-20T10:30:00",
  "documentsUploaded": 5,
  "barNumber": null,
  "lawFirm": null,
  "companyName": null,
  "dealSpecialty": null
}
Update Client
text
PUT /api/admin/clients/{id}
Request Body (all fields optional)

json
{
  "fullName": "Updated Name",
  "email": "new@example.com",
  "username": "newusername",
  "role": "DEAL_MAKER"
}
Response – Updated client object (same structure as GET by ID).

Delete Client
text
DELETE /api/admin/clients/{id}
Response

json
{
  "clientId": 1,
  "message": "Client deleted successfully"
}
⚠️ Note: Deletion may fail with 409 Conflict if the client has associated audit logs or documents. This is a known limitation – we recommend implementing a "deactivate" flag instead.

Filter Clients by Role
text
GET /api/admin/clients/by-role?role=DEAL_MAKER&page=0&size=10
Response – Paginated list of clients with the specified role.

Client Summary Statistics
text
GET /api/admin/clients/reports/summary
Response

json
{
  "totalClients": 25,
  "totalUsers": 18,
  "totalLegalPractitioners": 3,
  "totalDealMakers": 4,
  "totalDocumentsUploaded": 42,
  "clientsRegisteredLast30Days": 7,
  "clientsWithNoDocuments": 10
}
📊 Admin Dashboard
Provides a single endpoint with all KPIs and recent activity.

text
GET /api/admin/dashboard
Response

json
{
  "stats": {
    "totalClients": 25,
    "totalDocuments": 42,
    "totalReports": 38,
    "activeClients": 20,
    "pendingDocuments": 5,
    "failedDocuments": 2,
    "processedDocuments": 35,
    "highRiskReports": 7
  },
  "clients": [
    {
      "clientId": 1,
      "fullName": "John Doe",
      "email": "john@example.com",
      "role": "USER",
      "registeredAt": "2026-04-20T10:30:00",
      "documentCount": 5
    }
  ],
  "recentDocuments": [
    {
      "documentId": 101,
      "title": "contract.pdf",
      "status": "ANALYZED",
      "ingestionSource": "UPLOAD",
      "uploadedAt": "2026-04-20T09:15:00",
      "fileUrl": "https://...",
      "clientId": 1,
      "clientName": "John Doe"
    }
  ],
  "reports": [
    {
      "reportId": 201,
      "documentId": 101,
      "documentTitle": "contract.pdf",
      "riskLevel": "HIGH",
      "analysisStatus": "COMPLETED",
      "aiRecommendation": "Review clause 4.2",
      "generatedAt": "2026-04-20T09:30:00",
      "modelVersion": "gemini-1.5-pro"
    }
  ],
  "roleDistribution": [
    { "role": "USER", "count": 18 },
    { "role": "DEAL_MAKER", "count": 4 },
    { "role": "LEGAL_PRACTITIONER", "count": 3 }
  ],
  "riskDistribution": [
    { "riskLevel": "LOW", "count": 20 },
    { "riskLevel": "MEDIUM", "count": 11 },
    { "riskLevel": "HIGH", "count": 7 }
  ],
  "uploadTrend": [
    { "year": 2026, "month": 3, "count": 12, "label": "Mar 2026" },
    { "year": 2026, "month": 4, "count": 30, "label": "Apr 2026" }
  ]
}
📜 Audit Logs (Admin Only)
All audit log endpoints are read‑only. No deletion or modification is allowed.

Get Recent Logs
text
GET /api/admin/audit/recent?limit=20
Response – Array of log entries, newest first.

json
[
  {
    "logId": 1042,
    "actionType": "LOGIN",
    "description": "Successful login: john@example.com",
    "ipAddress": "192.168.1.100",
    "createdAt": "2026-04-20T14:22:00",
    "clientId": 1,
    "adminId": null,
    "documentId": null
  }
]
Get All Logs
text
GET /api/admin/audit
Returns all audit records (use with pagination for large datasets).

Get Logs by Client
text
GET /api/admin/audit/client/{clientId}
Returns audit history for a specific client.

🚧 Features Under Development
The following endpoints exist but are not yet fully functional. Frontend integration should wait until these are stabilised.

Endpoint	Status	Notes
POST /api/documents/upload	🟡 Storage broken	File upload to cloud storage fails; manual DB inserts work for testing
GET /api/documents/{id}	🟢 Works with manual data	Returns metadata if document exists
GET /api/documents/client/{clientId}	🟢 Works with manual data	Lists documents for a client
GET /api/reports/{reportId}	🟡 Requires AI pipeline	Reports only exist if generated manually
GET /api/reports/{reportId}/download-pdf	🟡 Depends on report data	PDF generation works but needs report content


🛠️ Tech Stack
Layer	Technology
Framework	Spring Boot 3.3.4
Language	Java 21
Database	PostgreSQL 15 (Supabase)
ORM	Hibernate / JPA
Real‑time	WebSocket (STOMP over SockJS)
Security	BCrypt password hashing

Clone the repository

bash
git clone https://github.com/your-org/alis-backend.git
cd alis-backend
Configure the database
Copy application.properties.example to application.properties and fill in your PostgreSQL credentials.

Run the application

bash
mvn spring-boot:run

The API will be available at http://localhost:8081.

Test the API
Use the provided Postman collection (available in /docs/postman) or cURL examples above.

📮 Postman Collection
A complete Postman collection with all working endpoints is available in the /docs folder. Import it to quickly test the API.

