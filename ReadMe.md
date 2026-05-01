# ALIS — Legal Compliance API

> Backend API for the Automated Legal Intelligence System
> Built for POPIA & CPA compliance in South Africa

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Firebase](https://img.shields.io/badge/Firebase-Storage-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)

---

## 🌐 Base URL

| Environment | URL |
|-------------|-----|
| Local | `http://localhost:8081` |
| Production | `https://alis-backend.onrender.com` |

All endpoints are prefixed with `/api`.

---

## ✅ Production-Ready Features

| Feature | Status |
|---------|--------|
| User registration & login | ✅ Fully working |
| Admin client management (CRUD) | ✅ Fully working |
| Admin dashboard statistics | ✅ Fully working |
| Audit logging (read-only for admins) | ✅ Fully working |
| Real-time audit via WebSocket | ✅ Fully working |
| Document upload to Firebase Storage | ✅ Fully working |
| **Law rule extraction & CRUD** | ✅ Fully working |
| AI document analysis pipeline | 🚧 Under development |
| Compliance report generation | 🚧 Under development |

---

## 🔐 Authentication

### Register a New Client

Creates a new user account. Role defaults to `USER`.

POST /api/auth/register

text

**Request Body**

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
Login
Authenticates a user and returns their profile.

text
POST /api/auth/login
Request Body

json
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
Response 401 Unauthorized

json
{
  "message": "Invalid credentials",
  "success": false
}
📄 Document Upload (Firebase Storage)
Upload a legal document for AI compliance analysis.

text
POST /api/documents/upload
Form Data

Key	Type	Description
file	File	PDF, DOCX, or TXT — max 10 MB
clientId	Text	ID of the registered client
Example (cURL)

bash
curl -X POST http://localhost:8081/api/documents/upload \
  -F "file=@/path/to/document.pdf" \
  -F "clientId=1"
Response 200 OK

json
{
  "message": "Document uploaded successfully",
  "documentId": 3,
  "title": "document.pdf",
  "status": "PENDING",
  "fileUrl": "https://storage.googleapis.com/..."
}
Note: The returned fileUrl is a temporary signed URL valid for 1 hour. For permanent access, regenerate a signed URL or configure the bucket as public.

👥 Client Management (Admin Only)
All endpoints under /api/admin/clients are intended for administrators.
Role enforcement is not yet active — all requests are currently permitted.

List All Clients (Paginated)
text
GET /api/admin/clients?page=0&size=20&sort=createdAt&dir=desc
Param	Default	Description
page	0	Page number (zero-indexed)
size	20	Items per page
sort	createdAt	Sort field
dir	desc	Sort direction (asc or desc)
Response

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
  "totalPages": 1,
  "totalElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
Other Client Endpoints
Method	Endpoint	Description
GET	/api/admin/clients/{id}	Get client by ID
PUT	/api/admin/clients/{id}	Update client
DELETE	/api/admin/clients/{id}	Delete client
GET	/api/admin/clients/by-role?role=DEAL_MAKER	Filter by role
POST	/api/admin/clients/filter	Advanced filter (role, date range, search)
GET	/api/admin/clients/reports/summary	Summary statistics
GET	/api/admin/clients/reports/role-distribution	Role breakdown
GET	/api/admin/clients/reports/registration-trend?months=12	Monthly trend
GET	/api/admin/clients/reports/top-uploaders	Most active clients
GET	/api/admin/clients/reports/inactive	Clients with no documents
⚠️ Warning: DELETE /api/admin/clients/{id} may return 409 Conflict if the client has associated audit logs or documents. Consider implementing a soft-delete (deactivate flag) instead.

📊 Admin Dashboard
Single endpoint returning all KPIs and recent activity in one call.

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
  "clients": [ "..." ],
  "recentDocuments": [ "..." ],
  "reports": [ "..." ],
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
Read-only. No modification or deletion is permitted.

Method	Endpoint	Description
GET	/api/admin/audit	All audit logs
GET	/api/admin/audit/recent?limit=20	Most recent logs
GET	/api/admin/audit/client/{clientId}	Logs for a specific client
Sample Log Entry

json
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
⚖️ Law Rules (Compliance Extraction)
The system extracts compliance rules from legal Acts (only Chapters 2‑4) and stores them in the law_rule table.
A full CRUD API is provided for manual rule management.

Method	Endpoint	Description
GET	/api/rules	List all rules
GET	/api/rules/{id}	Get a single rule by ID
POST	/api/rules	Create a new rule (must reference a valid actId)
PUT	/api/rules/{id}	Update a rule
DELETE	/api/rules/{id}	Delete a rule
Sample Rule

json
{
  "ruleId": 169,
  "actId": 1,
  "actName": "Consumer Protection Act",
  "keyword": "must provide",
  "requirements": "A supplier must provide a written record of each transaction.",
  "riskLevel": "MEDIUM",
  "suggestion": "Implement a process to issue transaction records.",
  "edited": false
}
Rule extraction can also be triggered by placing a PDF in src/main/resources/acts/ and setting
rule.seeding.enabled=true in application.properties. Extracted rules are saved directly to the database.

🚧 Features Under Development
Endpoint	Status	Notes
GET /api/reports/{reportId}	🟡 In progress	Requires AI pipeline completion
GET /api/reports/{reportId}/download-pdf	🟡 In progress	PDF generation works; needs report data
AI analysis pipeline	🟡 In progress	Text extraction working; clause extraction & rule matching are next
🛠️ Tech Stack
Layer	Technology
Framework	Spring Boot 3.3.4
Language	Java 21
Database	PostgreSQL 15 (Supabase)
ORM	Hibernate / JPA
Cloud Storage	Firebase Storage
Real-time	WebSocket (STOMP over SockJS)
Security	BCrypt password hashing
🚀 Getting Started
1. Clone the repository

bash
git clone https://github.com/your-org/alis-backend.git
cd alis-backend
2. Configure the application

Copy application.properties.example to application.properties and fill in:

DB_URL, DB_USERNAME, DB_PASSWORD — PostgreSQL / Supabase credentials

Firebase service account JSON path

3. Run the application

bash
mvn spring-boot:run
The API will be available at http://localhost:8081.

4. Test the API

Use the Postman collection in /docs/postman or the cURL examples above.

📮 Postman Collection
A complete Postman collection with all working endpoints is available in the /docs folder.
Import it directly into Postman to start testing immediately.

📄 License
This project is proprietary and confidential. Unauthorized distribution is prohibited.