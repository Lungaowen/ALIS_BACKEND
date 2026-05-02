# ALIS Backend

ALIS is a Spring Boot backend for legal document upload, compliance analysis, reporting, and admin management.

This README describes the functions that are working in the current codebase.

## Stack

- Java 21
- Spring Boot 3.3.4
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL / Supabase
- Firebase Storage
- Groq API for AI analysis
- OpenPDF for report PDFs

## Base URLs

- Local: `http://localhost:8081`
- Health: `http://localhost:8081/health`
- Root: `http://localhost:8081/`

`server.port` defaults to `8081` locally and can be overridden with `PORT`.

## Working Functions

### Authentication

- User login
- Admin login
- Public registration for:
  - `USER`
  - `DEAL_MAKER`
  - `LEGAL_PRACTITIONER`
- JWT token generation and validation

### User Self-Service

Authenticated `USER`, `DEAL_MAKER`, and `LEGAL_PRACTITIONER` accounts can:

- update their own full name
- update their own username
- change their own password by providing `currentPassword` and `newPassword`

### Admin Functions

Authenticated `ADMIN` can:

- view the admin dashboard
- view audit logs
- list clients
- filter clients
- update client details
- delete a client
- view role distribution, registration trend, top uploaders, and inactive clients

The admin delete flow now removes the user's related documents, report records, file-linked data, and audit log rows before deleting the client record.

### Client Document Functions

Authenticated `USER`, `DEAL_MAKER`, and `LEGAL_PRACTITIONER` can:

- upload documents
- list their own documents
- fetch a single owned document
- fetch reports for an owned document
- download owned report PDFs

### Compliance Functions

Authenticated users can:

- trigger or re-trigger compliance analysis
- poll compliance status
- fetch the final compliance result for a document

### Reports

Authenticated users or admins can fetch:

- report by report ID
- reports by document ID
- reports by client ID
- report PDF download

### Legal Practitioner Rule Management

Authenticated `LEGAL_PRACTITIONER` users can:

- list rules
- view one rule
- create rules
- update rules
- delete rules

### Specialist Account APIs

Authenticated requests can also create:

- legal practitioners
- deal makers

These endpoints exist separately from `/api/auth/register`.

### Storage and Processing

- Firebase file upload is working
- duplicate-file detection is working
- document metadata persistence is working
- PDF report generation is wired
- AI analysis service is wired
- app startup does not require `GROQ_API_KEY`, but live AI analysis does

## Roles

### `ADMIN`

- `/api/admin/**`
- full client management
- dashboard and audit access

### `USER`

- register and log in
- upload documents
- run compliance analysis
- view reports
- update own profile

### `DEAL_MAKER`

- register and log in
- upload documents
- run compliance analysis
- view reports
- update own profile
- cannot manage rules

### `LEGAL_PRACTITIONER`

- register and log in
- upload documents
- run compliance analysis
- view reports
- update own profile
- can CRUD rules

## Security Rules

- Public:
  - `GET /`
  - `GET /health`
  - `POST /api/auth/login`
  - `POST /api/auth/register`
- Admin only:
  - `/api/admin/**`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
- Client roles only:
  - `/api/client/**`
- Legal practitioner only:
  - `/api/rules/**`
- Everything else requires authentication

## Main Endpoints

### Health

- `GET /`
- `GET /health`

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`

### Client Profile

- `PUT /api/client/profile`

### Admin

- `GET /api/admin/dashboard`
- `GET /api/admin/audit`
- `GET /api/admin/audit/recent`
- `GET /api/admin/audit/client/{clientId}`

### Admin Client Management

- `GET /api/admin/clients`
- `GET /api/admin/clients/{id}`
- `POST /api/admin/clients/filter`
- `GET /api/admin/clients/by-role`
- `GET /api/admin/clients/by-date`
- `GET /api/admin/clients/{id}/document-count`
- `PUT /api/admin/clients/{id}`
- `DELETE /api/admin/clients/{id}`
- `GET /api/admin/clients/reports/summary`
- `GET /api/admin/clients/reports/role-distribution`
- `GET /api/admin/clients/reports/registration-trend`
- `GET /api/admin/clients/reports/top-uploaders`
- `GET /api/admin/clients/reports/inactive`

### Client Document APIs

- `POST /api/client/documents/upload`
- `GET /api/client/documents`
- `GET /api/client/documents/{id}`
- `GET /api/client/documents/{documentId}/reports`
- `GET /api/client/reports/{reportId}/download`

### Compliance

- `POST /api/compliance/analyze/{documentId}`
- `GET /api/compliance/status/{documentId}`
- `GET /api/compliance/result/{documentId}`

### Reports

- `GET /api/reports/{reportId}`
- `GET /api/reports/document/{documentId}`
- `GET /api/reports/client/{clientId}`
- `GET /api/reports/{reportId}/download-pdf`

### Legal Practitioner Rules

- `GET /api/rules`
- `GET /api/rules/{id}`
- `POST /api/rules`
- `PUT /api/rules/{id}`
- `DELETE /api/rules/{id}`

### Specialist Account Controllers

- `GET /api/legal-practitioners`
- `GET /api/legal-practitioners/{id}`
- `POST /api/legal-practitioners`
- `GET /api/dealmakers`
- `GET /api/dealmakers/{id}`
- `POST /api/dealmakers`

### General Document Controller

These routes also exist and are useful for internal or admin-style flows:

- `POST /api/documents/upload`
- `GET /api/documents/{id}`
- `GET /api/documents/client/{clientId}`
- `GET /api/documents/all`
- `DELETE /api/documents/{id}`
- `GET /api/documents/{id}/download`

### Test Upload Endpoint

- `POST /api/test/upload`

This is a lightweight testing endpoint and should not be treated as the primary app upload API.

## Request Examples

### Register a normal user

```json
POST /api/auth/register
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123",
  "role": "USER"
}
```

### Register a deal maker

```json
POST /api/auth/register
{
  "fullName": "Anele Deal",
  "email": "dealmaker@example.com",
  "password": "DealPass!2026",
  "role": "DEAL_MAKER",
  "companyName": "Blue Horizon Capital",
  "dealSpecialty": "SME acquisitions"
}
```

### Register a legal practitioner

```json
POST /api/auth/register
{
  "fullName": "Lebo Practitioner",
  "email": "legal@example.com",
  "password": "LegalPass!2026",
  "role": "LEGAL_PRACTITIONER",
  "barNumber": "LP-2026-001",
  "lawFirm": "Mokoena Legal"
}
```

### Login

```json
POST /api/auth/login
{
  "email": "legal@example.com",
  "password": "LegalPass!2026"
}
```

### Update own profile

```json
PUT /api/client/profile
{
  "fullName": "Lebo Practitioner Updated",
  "username": "lebo_updated",
  "currentPassword": "LegalPass!2026",
  "newPassword": "LegalPass!2026#Updated"
}
```

### Create a rule

```json
POST /api/rules
{
  "actId": 1,
  "keyword": "cooling-off period",
  "requirements": "The agreement must clearly disclose the cooling-off period.",
  "riskLevel": "MEDIUM",
  "suggestion": "Add a dedicated cooling-off clause."
}
```

## Environment Variables

Required for boot:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ALIS_JWT_SECRET`
- `FIREBASE_BUCKET_NAME`

Optional but strongly recommended:

- `FIREBASE_SERVICE_ACCOUNT`
- `GROQ_API_KEY`
- `ALIS_SEED_ADMIN_EMAIL`
- `ALIS_SEED_ADMIN_PASSWORD`
- `ALIS_SEED_ADMIN_NAME`
- `ALIS_CORS_ALLOWED_ORIGIN_PATTERNS`
- `ALIS_JWT_EXPIRATION`
- `DDL_MODE`
- `JPA_SHOW_SQL`
- `PORT`

## Local Run

### 1. Run tests

```bash
mvn test
```

### 2. Start the app

```bash
mvn spring-boot:run
```

### 3. Or run the packaged jar

```bash
mvn -DskipTests package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Render Deployment

The project includes a Dockerfile and is suitable for Render deployment.

Recommended Render env vars:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ALIS_JWT_SECRET`
- `FIREBASE_BUCKET_NAME`
- `FIREBASE_SERVICE_ACCOUNT` or `/etc/secrets/firebase.json`
- `GROQ_API_KEY`
- `ALIS_SEED_ADMIN_EMAIL`
- `ALIS_SEED_ADMIN_PASSWORD`
- `ALIS_SEED_ADMIN_NAME`
- `PORT`

Recommended health check path:

- `/health`

## Notes and Current Limits

- Swagger is admin-protected by the current security config.
- Admin login returns the ID in the `clientId` field for compatibility with the existing response DTO.
- There is no public `GET /api/acts` endpoint yet.
- Password change exists for client roles through `/api/client/profile`.
- There is still no dedicated admin password-change endpoint.

## Verification Status

The codebase currently passes the automated test suite with:

- `mvn test`

Recent verified backend changes include:

- role-based public registration
- user self-service profile update
- password change for client roles
- safer admin client deletion

