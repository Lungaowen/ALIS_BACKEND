# ALIS Frontend API Brief for Lovable / JavaFX

Use this document as the source of truth for two clients that consume the ALIS backend:

1. `Admin Desktop Portal` built in JavaFX for `ADMIN`
2. `Animated Client Website` for `USER`, `LEGAL_PRACTITIONER`, and `DEAL_MAKER`

Backend base URL for local development:

```text
http://localhost:8081
```

Swagger:

```text
http://localhost:8081/swagger-ui/index.html
```

## Product Goal

Build two separate frontends against the same API:

1. A serious, operations-focused `Admin JavaFX Portal`
2. A polished, animated client-facing website with role-aware dashboards

The client website must present different UI experiences for:

- `LEGAL_PRACTITIONER`
- `DEAL_MAKER`
- `USER`

Important business rule:

- `LEGAL_PRACTITIONER` can create, read, update, and delete compliance rules
- `DEAL_MAKER` cannot manage rules
- `LEGAL_PRACTITIONER` and `DEAL_MAKER` can both upload documents, run compliance checks, view results, and download reports

## Authentication

All protected endpoints use Bearer JWT tokens.

Request header:

```http
Authorization: Bearer <jwt-token>
```

Login endpoint:

```http
POST /api/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "admin@example.com",
  "password": "ChangeThisToAStrongPassword123!"
}
```

Success response shape:

```json
{
  "message": "Login successful",
  "clientId": 14,
  "email": "legal.practitioner@example.com",
  "fullName": "Lebo Practitioner",
  "role": "LEGAL_PRACTITIONER",
  "success": true,
  "token": "jwt-token-here"
}
```

Notes:

- The field is named `clientId` for every role, including admin. Treat it as the authenticated actor ID.
- There is no refresh token endpoint.
- There is no logout endpoint. Frontends should log out by clearing the stored token.

## Public Endpoints

These do not require authentication:

- `GET /`
- `GET /health`
- `POST /api/auth/login`
- `POST /api/auth/register`
- Swagger endpoints

Registration note:

- `POST /api/auth/register` creates a plain `USER` only
- It does not create `LEGAL_PRACTITIONER` or `DEAL_MAKER`

## Role Matrix

| Capability | ADMIN | USER | LEGAL_PRACTITIONER | DEAL_MAKER |
|---|---:|---:|---:|---:|
| Login | Yes | Yes | Yes | Yes |
| View admin dashboard | Yes | No | No | No |
| View audit logs | Yes | No | No | No |
| Manage clients | Yes | No | No | No |
| Upload documents | No UI need | Yes | Yes | Yes |
| View own documents | No UI need | Yes | Yes | Yes |
| Trigger compliance analysis | Not primary | Yes | Yes | Yes |
| Poll compliance status | Not primary | Yes | Yes | Yes |
| View own compliance reports | No UI need | Yes | Yes | Yes |
| Download own report PDF | No UI need | Yes | Yes | Yes |
| Create rules | No | No | Yes | No |
| Edit rules | No | No | Yes | No |
| Delete rules | No | No | Yes | No |

## Security Rules That Matter to the UI

- `/api/admin/**` is `ADMIN` only
- `/api/client/**` is `USER`, `LEGAL_PRACTITIONER`, or `DEAL_MAKER`
- `/api/rules/**` is `LEGAL_PRACTITIONER` only
- `/api/compliance/**` requires authentication and is available to authenticated roles

## Frontend Architecture Brief

### 1. Admin JavaFX Portal

Audience: internal admin staff

Style:

- Desktop-first
- Minimal animation
- Dense data tables
- Fast filters
- Clear role badges
- Export and drill-down friendly

Primary screens:

1. Login
2. Dashboard
3. Client management
4. Client detail
5. Audit log explorer
6. Reports and platform analytics

### 2. Animated Client Website

Audience: external users, legal practitioners, and deal makers

Style:

- Motion should feel premium, not decorative noise
- Use soft page transitions, progress animations, upload-state motion, and chart reveals
- The site should feel modern and confident

Shared screens:

1. Landing / login
2. Role-aware dashboard
3. Upload center
4. My documents
5. Compliance status / result
6. Report detail
7. Report download

Role-specific experience:

#### USER UI

- Simple dashboard
- Upload document
- View own documents
- View compliance results
- Download reports

#### LEGAL_PRACTITIONER UI

- More analytical dashboard than USER
- Show rule insights and regulatory context
- Add a dedicated `Rules Workspace`
- Can CRUD rules
- Can upload documents, run compliance checks, view results, download reports

Suggested sections:

- Overview
- Upload and analyze
- My documents
- Reports
- Rules workspace

#### DEAL_MAKER UI

- Different visual language from legal practitioner
- Focus on decision flow, turnaround, risk flags, and report downloads
- No rules management UI

Suggested sections:

- Overview
- Upload and analyze
- My documents
- Reports
- Deal readiness / risk summary

## Core Auth API

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "someone@example.com",
  "password": "secret"
}
```

Response:

```json
{
  "message": "Login successful",
  "clientId": 14,
  "email": "someone@example.com",
  "fullName": "Someone Name",
  "role": "DEAL_MAKER",
  "success": true,
  "token": "..."
}
```

### Register plain USER

```http
POST /api/auth/register
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Client User",
  "email": "client@example.com",
  "password": "StrongPassword123!"
}
```

## Account Provisioning API

These are useful if the desktop portal or a protected admin workflow creates specialist users.

### Create Legal Practitioner

```http
POST /api/legal-practitioners
Content-Type: application/json
Authorization: Bearer <jwt>
```

Body:

```json
{
  "fullName": "Lebo Practitioner",
  "email": "legal@example.com",
  "password": "LegalPass!2026",
  "barNumber": "LP-2026-001",
  "lawFirm": "Mokoena Legal"
}
```

Response shape:

```json
{
  "clientId": 14,
  "fullName": "Lebo Practitioner",
  "email": "legal@example.com",
  "username": "lebopractitioner_1234",
  "role": "LEGAL_PRACTITIONER",
  "createdAt": "2026-04-29T00:49:25",
  "barNumber": "LP-2026-001",
  "lawFirm": "Mokoena Legal"
}
```

### Create Deal Maker

```http
POST /api/dealmakers
Content-Type: application/json
Authorization: Bearer <jwt>
```

Body:

```json
{
  "fullName": "Anele Deal",
  "email": "dealmaker@example.com",
  "password": "DealPass!2026",
  "companyName": "Blue Horizon Capital",
  "dealSpecialty": "SME acquisitions"
}
```

Response shape:

```json
{
  "clientId": 15,
  "fullName": "Anele Deal",
  "email": "dealmaker@example.com",
  "username": "aneledeal_4567",
  "role": "DEAL_MAKER",
  "createdAt": "2026-04-29T00:51:10",
  "companyName": "Blue Horizon Capital",
  "dealSpecialty": "SME acquisitions"
}
```

Important backend note:

- These endpoints are authenticated but are not currently limited to admin only by route policy
- Frontend should expose them only in trusted flows

## Client Document API

All routes below are for `USER`, `LEGAL_PRACTITIONER`, and `DEAL_MAKER`.

### Upload document

```http
POST /api/client/documents/upload
Content-Type: multipart/form-data
Authorization: Bearer <jwt>
```

Form field:

- `file`: PDF or supported upload

Response:

```json
{
  "message": "Document uploaded successfully",
  "documentId": 9,
  "title": "CPA_Act.pdf",
  "status": "PENDING",
  "fileUrl": "https://..."
}
```

Important workflow note:

- Upload automatically starts the AI pipeline
- The explicit analyze endpoint can still be used to re-run analysis

### List my documents

```http
GET /api/client/documents
Authorization: Bearer <jwt>
```

Response item shape:

```json
{
  "documentId": 9,
  "title": "CPA_Act.pdf",
  "status": "ANALYZED",
  "ingestionSource": "MANUAL",
  "uploadedAt": "2026-04-29T00:50:02",
  "filePath": "clients/14/CPA_Act.pdf",
  "fileUrl": "https://...",
  "clientId": 14
}
```

### Get one owned document

```http
GET /api/client/documents/{documentId}
Authorization: Bearer <jwt>
```

### Get reports for one owned document

```http
GET /api/client/documents/{documentId}/reports
Authorization: Bearer <jwt>
```

Response item shape:

```json
{
  "reportId": 458,
  "documentId": 9,
  "documentTitle": "CPA_Act.pdf",
  "clientId": 14,
  "clientName": "Lebo Practitioner",
  "riskLevel": "LOW",
  "analysisStatus": "COMPLETED",
  "similarityScore": 80.00,
  "aiRecommendation": "Compliant with low legal risk...",
  "aiExplanation": "The document aligns with the requirements...",
  "generatedAt": "2026-04-29T00:50:15",
  "modelVersion": "llama-3.3-70b-versatile",
  "lawRuleId": 1,
  "lawRuleKeyword": "consumer rights",
  "actName": "Consumer Protection Act"
}
```

### Download owned report PDF

```http
GET /api/client/reports/{reportId}/download
Authorization: Bearer <jwt>
```

This returns `application/pdf`.

## Compliance API

Use this for progress tracking and re-analysis.

### Trigger or re-trigger analysis

```http
POST /api/compliance/analyze/{documentId}
Authorization: Bearer <jwt>
```

Response:

```json
{
  "message": "Compliance analysis started",
  "documentId": 9,
  "title": "CPA_Act.pdf",
  "currentStatus": "PENDING",
  "note": "Poll GET /api/compliance/status/9 to check progress"
}
```

### Poll analysis status

```http
GET /api/compliance/status/{documentId}
Authorization: Bearer <jwt>
```

Possible in-progress response:

```json
{
  "documentId": 9,
  "title": "CPA_Act.pdf",
  "documentStatus": "PENDING",
  "reportReady": false,
  "message": "Analysis in progress - please wait"
}
```

Possible completed response:

```json
{
  "documentId": 9,
  "title": "CPA_Act.pdf",
  "documentStatus": "ANALYZED",
  "analysisStatus": "COMPLETED",
  "riskLevel": "LOW",
  "similarityScore": 80.00,
  "reportId": 458,
  "reportReady": true
}
```

### Get final compliance result

```http
GET /api/compliance/result/{documentId}
Authorization: Bearer <jwt>
```

Returns a single `ReportInfoDTO` object with the same shape as the report item above.

## Legal Practitioner Rules API

These routes are `LEGAL_PRACTITIONER` only.

### List rules

```http
GET /api/rules
Authorization: Bearer <jwt>
```

### Get one rule

```http
GET /api/rules/{id}
Authorization: Bearer <jwt>
```

### Create rule

```http
POST /api/rules
Content-Type: application/json
Authorization: Bearer <jwt>
```

Body:

```json
{
  "actId": 1,
  "keyword": "cooling-off period",
  "requirements": "The agreement must disclose the cooling-off period clearly.",
  "riskLevel": "MEDIUM",
  "suggestion": "Add a dedicated cooling-off clause."
}
```

### Update rule

```http
PUT /api/rules/{id}
Content-Type: application/json
Authorization: Bearer <jwt>
```

Body:

```json
{
  "requirements": "Updated requirement text",
  "riskLevel": "HIGH",
  "suggestion": "Revise the clause wording."
}
```

### Delete rule

```http
DELETE /api/rules/{id}
Authorization: Bearer <jwt>
```

Rule response shape:

```json
{
  "ruleId": 1,
  "actId": 1,
  "actName": "Consumer Protection Act",
  "keyword": "cooling-off period",
  "requirements": "The agreement must disclose the cooling-off period clearly.",
  "riskLevel": "MEDIUM",
  "suggestion": "Add a dedicated cooling-off clause.",
  "edited": false
}
```

Current backend limitation:

- There is no public `GET /api/acts` endpoint right now
- Rule forms need a fixed act list or a backend enhancement
- The seeded deployment currently centers on `Consumer Protection Act`

## Admin JavaFX API

These routes are `ADMIN` only and should power the desktop portal.

### Dashboard

```http
GET /api/admin/dashboard
Authorization: Bearer <jwt>
```

Response shape:

```json
{
  "stats": {},
  "clients": [],
  "recentDocuments": [],
  "reports": [],
  "roleDistribution": [],
  "riskDistribution": [],
  "uploadTrend": []
}
```

Treat the dashboard as a compound analytics payload.

### Audit log

```http
GET /api/admin/audit
GET /api/admin/audit/recent?limit=20
GET /api/admin/audit/client/{clientId}
Authorization: Bearer <jwt>
```

Audit entry shape:

```json
{
  "logId": 22,
  "actionType": "ANALYSIS_RUN",
  "description": "Groq compliance analysis triggered for: CPA_Act.pdf",
  "ipAddress": "127.0.0.1",
  "createdAt": "2026-04-29T00:50:02",
  "clientId": 14,
  "adminId": null,
  "documentId": 9
}
```

### Client management

```http
GET /api/admin/clients?page=0&size=20&sort=createdAt&dir=desc
GET /api/admin/clients/{id}
POST /api/admin/clients/filter?page=0&size=20
GET /api/admin/clients/by-role?role=LEGAL_PRACTITIONER&page=0&size=20
GET /api/admin/clients/by-date?from=2026-04-01T00:00:00&to=2026-04-30T23:59:59&page=0&size=20
GET /api/admin/clients/{id}/document-count
PUT /api/admin/clients/{id}
DELETE /api/admin/clients/{id}
Authorization: Bearer <jwt>
```

Useful admin detail shape:

```json
{
  "clientId": 14,
  "fullName": "Lebo Practitioner",
  "email": "legal@example.com",
  "username": "lebopractitioner_1234",
  "role": "LEGAL_PRACTITIONER",
  "createdAt": "2026-04-29T00:49:25",
  "documentsUploaded": 1,
  "barNumber": "LP-2026-001",
  "lawFirm": "Mokoena Legal",
  "companyName": null,
  "dealSpecialty": null
}
```

Filter body shape:

```json
{
  "role": "DEAL_MAKER",
  "registeredFrom": "2026-04-01T00:00:00",
  "registeredTo": "2026-04-30T23:59:59",
  "searchQuery": "Anele"
}
```

Update body shape:

```json
{
  "fullName": "Updated Name",
  "email": "updated@example.com",
  "username": "updated_user",
  "role": "DEAL_MAKER",
  "barNumber": null,
  "lawFirm": null,
  "companyName": "Blue Horizon Capital",
  "dealSpecialty": "SME acquisitions"
}
```

### Admin reports

```http
GET /api/admin/clients/reports/summary
GET /api/admin/clients/reports/role-distribution
GET /api/admin/clients/reports/registration-trend?months=12
GET /api/admin/clients/reports/top-uploaders?page=0&size=10
GET /api/admin/clients/reports/inactive
Authorization: Bearer <jwt>
```

## Recommended UI Flows

### Client website: upload and analyze

1. Login
2. Send token with every protected request
3. Upload document through `/api/client/documents/upload`
4. Immediately navigate to a progress screen
5. Poll `/api/compliance/status/{documentId}` every few seconds
6. When `reportReady=true`, open result view
7. Fetch details from either:
   - `/api/compliance/result/{documentId}`
   - or `/api/client/documents/{documentId}/reports`
8. Offer PDF download through `/api/client/reports/{reportId}/download`

### Legal practitioner rule management

1. Login as `LEGAL_PRACTITIONER`
2. Load `/api/rules`
3. Show a searchable table with create and edit modal forms
4. Prevent this entire section from rendering for `DEAL_MAKER` and `USER`

### Admin desktop portal

1. Login as `ADMIN`
2. Load `/api/admin/dashboard`
3. Load recent audit activity
4. Use paginated client management tables
5. Offer filters by role, date range, and search term

## UX Rules for Lovable

### Global

- Build a role-aware app shell after login
- Route by role immediately after auth
- Show friendly empty states
- Show upload, analysis, success, and failure states clearly
- Use token-aware API helpers
- Handle `401` by redirecting to login
- Handle `403` by showing a role-permission message, not a generic crash

### Legal practitioner experience

- Use a compliance and governance tone
- Include rule inventory, risk context, and edit affordances
- Highlight that this role can manage rules

### Deal maker experience

- Use a decision-support tone
- Emphasize speed, risk status, report readiness, and download actions
- Do not render any rule CRUD components

### Admin JavaFX experience

- Use table-heavy views with persistent filters
- Include dashboard charts and recent activity lists
- Support large datasets and paging

## Known Backend Constraints

1. `AuthResponse.clientId` also represents the admin ID during admin login
2. No refresh token flow
3. No logout endpoint
4. No `GET /api/acts` endpoint for rule-form dropdown data
5. Specialist account creation endpoints are authenticated but not admin-only by route policy
6. `POST /api/auth/register` creates `USER` only

## Copy/Paste Build Prompt

Use this prompt inside Lovable or any frontend builder:

```text
Build two frontends for the ALIS compliance platform against a Spring Boot backend at http://localhost:8081.

Frontend 1: an Admin JavaFX desktop portal for ADMIN users. It must include login, dashboard analytics, client management, filters, audit logs, role distribution, registration trends, top uploaders, and inactive-client reporting. Use a dense operational UI with tables, charts, and drill-down views.

Frontend 2: an animated client-facing website for USER, LEGAL_PRACTITIONER, and DEAL_MAKER roles. After login, route users to role-specific dashboards. USER gets a simple upload-and-report workflow. LEGAL_PRACTITIONER gets a more analytical UI plus a Rules Workspace that can CRUD /api/rules. DEAL_MAKER gets a distinct UI focused on turnaround, risk status, and report downloads, but no rules management.

Use Bearer JWT authentication from POST /api/auth/login. Store the token and send it on all protected requests. Upload documents with POST /api/client/documents/upload, poll GET /api/compliance/status/{documentId}, fetch report results from GET /api/compliance/result/{documentId} or GET /api/client/documents/{documentId}/reports, and download PDFs from GET /api/client/reports/{reportId}/download.

The UI must gracefully handle 401 and 403 responses, show polished loading/progress states, and keep the legal practitioner and deal maker experiences visually distinct.
```
