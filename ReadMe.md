# ALIS Backend

**Automated Legal Intelligence System (ALIS)** — a Spring Boot REST API that ingests legal documents, runs AI-powered rule extraction and compliance analysis, stores files in Firebase, and serves role-based dashboards for Admins, Legal Practitioners, Dealmakers, and Clients.

> Repository: [`Lungaowen/ALIS_BACKEND`](https://github.com/Lungaowen/ALIS_BACKEND)

---

## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Key Modules (Per-File Scan)](#key-modules-per-file-scan)
5. [Getting Started](#getting-started)
6. [Environment Variables](#environment-variables)
7. [Running Locally](#running-locally)
8. [Docker & Deployment](#docker--deployment)
9. [API Documentation](#api-documentation)
10. [Testing](#testing)
11. [Security Notes](#security-notes)

---

## Tech Stack

### Language & Runtime
- **Java 21** (Eclipse Temurin)
- **Maven** (wrapper included: `mvnw` / `mvnw.cmd`)

### Frameworks & Core Libraries
| Concern | Library | Version |
|---|---|---|
| Application framework | Spring Boot | 3.3.4 |
| REST / MVC | `spring-boot-starter-web` | 3.3.4 |
| Persistence | Spring Data JPA + Hibernate | 3.3.4 |
| Security | Spring Security + JWT (`jjwt`) | 3.3.4 / 0.11.5 |
| Real-time | Spring WebSocket (STOMP) | 3.3.4 |
| API Docs | springdoc-openapi (Swagger UI) | 2.6.0 |
| Lombok | `org.projectlombok:lombok` | (Spring-managed) |
| Env loader (dev) | `io.github.cdimascio:java-dotenv` | 5.2.2 |

### Databases
- **PostgreSQL** (production / Supabase) — `org.postgresql:postgresql`
- **H2** (in-memory, for local/dev/test) — `com.h2database:h2`

### Document & File Processing
- **Apache PDFBox** 2.0.30 — PDF text extraction
- **OpenPDF** 3.0.3 — PDF generation
- **Apache POI (`poi-ooxml`)** 5.2.5 — DOCX/XLSX parsing
- **Firebase Admin SDK** 9.3.0 — Cloud Storage for uploaded documents

### AI Integration
- **Groq API** (OpenAI-compatible) — default model `llama-3.3-70b-versatile`
- External **Python microservice** (URL configurable via `ALIS_PYTHON_SERVICE_API_URL`) for advanced rule extraction

### DevOps
- **Docker** (multi-stage build: Maven → JRE Alpine)
- **docker-compose** (local orchestration)
- **Render** (production hosting — `render.yaml` provided)

### Frontend Helpers (bundled)
- `frontend-api/alisApi.js` — JS client wrapper
- `frontend-api/exampleUsage.jsx` — React example
- `alis-api.json` — OpenAPI spec
- `LOVABLE_API_BRIEF.md` — frontend integration brief

---

## Architecture Overview

```
                     ┌─────────────────────────┐
                     │   Web / Mobile Client   │
                     └────────────┬────────────┘
                                  │ HTTPS + JWT
                                  ▼
         ┌────────────────────────────────────────────┐
         │           Spring Boot REST API             │
         │                                            │
         │  Controllers ─► Services ─► Repositories   │
         │       │             │             │        │
         │       │             ▼             ▼        │
         │       │      AI / Firebase /  PostgreSQL   │
         │       │      Python service                │
         │       ▼                                    │
         │  WebSocket (STOMP) ── live audit log feed  │
         └────────────────────────────────────────────┘
                                  │
                  ┌───────────────┼───────────────┐
                  ▼               ▼               ▼
              Postgres       Firebase         Groq AI /
             (Supabase)      Storage         Python svc
```

### Roles
Defined in `enums/Role.java` — typically `ADMIN`, `LEGAL_PRACTITIONER`, `DEALMAKER`, `CLIENT`. Authorization is enforced through `JwtAuthenticationFilter` + Spring Security.

---

## Project Structure

```
ALIS_BACKEND/
├── Dockerfile                       # Multi-stage Maven → JRE build
├── docker-compose.yml               # Local stack
├── pom.xml                          # Maven dependencies
├── mvnw / mvnw.cmd / .mvn/          # Maven wrapper
├── alis-api.json                    # OpenAPI specification
├── LOVABLE_API_BRIEF.md             # Frontend integration brief
├── ReadMe.md / HELP.md              # Original docs
├── frontend-api/                    # JS/React client samples
│   ├── alisApi.js
│   └── exampleUsage.jsx
└── src/
    ├── main/
    │   ├── java/za/ac/alis/
    │   │   ├── DemoApplication.java         # @SpringBootApplication entry
    │   │   ├── ActiveProfiles.java          # Profile helper
    │   │   ├── controller/                  # 14 REST controllers
    │   │   ├── service/                     # Business logic (17 services)
    │   │   ├── repo/                        # Spring Data JPA repositories
    │   │   ├── entities/                    # JPA @Entity classes
    │   │   ├── dto/                         # Request/response DTOs
    │   │   ├── projections/                 # JPA interface projections
    │   │   ├── queries/                     # Native/JPQL query constants
    │   │   ├── enums/                       # Domain enums
    │   │   ├── security/                    # JwtUtil, JwtAuthenticationFilter
    │   │   ├── webSocket/                   # STOMP WebSocketConfig
    │   │   └── utils/                       # File name helpers
    │   └── resources/
    │       ├── application.properties       # Base config
    │       ├── application-dev.properties   # Dev profile
    │       ├── application-prod.properties  # Prod profile
    │       ├── application-test.properties  # Test profile
    │       ├── render.yaml                  # Render deployment manifest
    │       └── CPA_Act.pdf                  # Seed legal document
    └── test/java/za/ac/alis/
        ├── demo/DemoApplicationTests.java
        ├── security/JwtUtilTests.java
        └── service/                         # Service unit tests
```

---

## Key Modules (Per-File Scan)

### Entry Point
| File | Purpose |
|---|---|
| `DemoApplication.java` | `@SpringBootApplication` — bootstraps the Spring context |
| `ActiveProfiles.java` | Utility for resolving the active Spring profile |

### Controllers (`controller/`)
| Controller | Responsibility |
|---|---|
| `AuthController` | Register, login, JWT issuance |
| `AdminDashboardController` | Aggregated stats for admin UI |
| `AuditLogController` | Query/stream audit events |
| `ClientController` | Client CRUD (admin view) |
| `ClientProfileController` | Authenticated client self-profile |
| `ClientDocumentController` | Client-scoped document operations |
| `DealmakerController` | Dealmaker user management |
| `LegalPractitionerController` | Legal practitioner management |
| `DocumentController` | Upload, list, fetch, update documents |
| `LawRuleController` | Manage law rules used in compliance checks |
| `ReportController` | Generate / retrieve summary reports |
| `HealthController` | `/health` endpoint (used by Render) |
| `TestUploadController` | Diagnostic upload endpoint |

### Services (`service/`)
| Service | Responsibility |
|---|---|
| `AdminClientService` / `AdminDashboardService` / `AdminReportService` | Admin-side aggregations |
| `AuditLogService` + `AuditWebSocketService` | Audit logging + STOMP push |
| `ClientService` / `ClientServiceINT` | Client domain logic + interface |
| `DealMakerService` / `LegalPractitionerService` | Per-role user services |
| `DocumentService` | Upload pipeline, persistence, retrieval |
| `FirebaseStorageService` | Wraps Firebase Admin SDK for file storage |
| `TextExtractionService` | Extracts text from PDF/DOCX (PDFBox + POI) |
| `RuleExtractionService` | Sends documents to Groq / Python service for rule extraction |
| `RuleSeederService` + `seed/ActRuleSeeder` | Seeds baseline legal rules at startup |
| `LawRuleService` | CRUD for law rules |
| `SummaryReportService` | Builds compliance summary reports (OpenPDF) |

### Data Layer
- **`entities/`** — JPA entities: `Admin`, `Client`, `DealMaker`, `LegalPractitioner`, `Document`, `DocumentContent`, `FileMetadata`, `Act`, `Clause`, `LawRul`, `AuditLog`, `SummaryReport`.
- **`repo/`** — Spring Data repositories matching each entity.
- **`projections/`** — Interface-based projections for efficient dashboard reads (`DashboardStats`, `MonthlyCount`, `RiskStat`, `TopUploader`, etc.).
- **`queries/`** — Centralised JPQL/native query string constants per domain.
- **`dto/`** — Request/response payloads (kept separate from entities).
- **`enums/`** — `Role`, `RiskLevel`, `RiskFlag`, `ComplianceStatus`, `AnalysisStatus`, `DocumentStat`, `IngestionSource`, `ActionType`.

### Security (`security/`)
- `JwtUtil` — token generation, parsing, validation (HMAC, secret from `ALIS_JWT_SECRET`).
- `JwtAuthenticationFilter` — extracts/validates JWT per request and populates the Spring `SecurityContext`.

### Real-time (`webSocket/`)
- `WebSocketConfig` — registers STOMP endpoints (used by `AuditWebSocketService` for live audit-log streaming).

### Utilities (`utils/`)
- `FileNameGenerator` — produces unique storage names.
- `FilenameSanitizer` — strips unsafe characters from uploads.

### Tests (`src/test/...`)
- `DemoApplicationTests` — Spring context smoke test.
- `JwtUtilTests` — token sign/verify edge cases.
- `AdminClientServiceTests`, `ClientServiceTests`, `SummaryReportServiceTests`.

---

## Getting Started

### Prerequisites
- **Java 21**
- **Maven 3.9+** (or use the bundled `./mvnw`)
- **PostgreSQL 14+** (or use Supabase) — or H2 for quick start
- **Firebase project** with a service account JSON + Storage bucket
- **Groq API key** (optional — required for AI rule extraction)

### Clone
```bash
git clone https://github.com/Lungaowen/ALIS_BACKEND.git
cd ALIS_BACKEND
```

---

## Environment Variables

Create a `config/.env` (used by docker-compose) or export in your shell.

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | ✅ | JDBC URL, e.g. `jdbc:postgresql://host:5432/alis` |
| `DB_USERNAME` | ✅ | DB user |
| `DB_PASSWORD` | ✅ | DB password |
| `ALIS_JWT_SECRET` | ✅ | HMAC secret for signing JWTs (long random string) |
| `ALIS_JWT_EXPIRATION` |  | Token TTL in ms (default `86400000` = 24h) |
| `SUPABASE_JWT_SECRET` | ✅ | Used to validate Supabase-issued tokens |
| `FIREBASE_BUCKET_NAME` | ✅ | Firebase Storage bucket name |
| `FIREBASE_SERVICE_ACCOUNT` |  | Full service-account JSON (string), or mount file |
| `GROQ_API_KEY` |  | Groq API key for AI features |
| `GROQ_MODEL` |  | Default `llama-3.3-70b-versatile` |
| `GROQ_API_URL` |  | Default Groq OpenAI-compatible endpoint |
| `ALIS_PYTHON_SERVICE_API_URL` |  | External Python rule-extraction service |
| `ALIS_SEED_ADMIN_NAME/EMAIL/PASSWORD` |  | Bootstrap admin user on first run |
| `ALIS_CORS_ALLOWED_ORIGIN_PATTERNS` |  | Comma-separated origins (default `*`) |
| `PORT` |  | HTTP port (default `8080`) |
| `SPRING_PROFILES_ACTIVE` |  | `dev` \| `prod` \| `test` |

---

## Running Locally

### Dev profile (verbose logs, Swagger on, Hibernate `update`)
```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

App boots at `http://localhost:8080` and Swagger UI is at `http://localhost:8080/swagger-ui.html`.

### Build a runnable JAR
```bash
./mvnw clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

---

## Docker & Deployment

### Local with docker-compose
```bash
# Place Firebase JSON at ./config/firebase-service-account.json
# Place env vars in ./config/.env
docker compose up --build
```

### Render (production)
The repo ships with `src/main/resources/render.yaml`. Create a new Render service from the repo; set the `sync: false` secrets in the Render dashboard:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `FIREBASE_SERVICE_ACCOUNT`, `FIREBASE_BUCKET_NAME`
- `GROQ_API_KEY`
- `ALIS_JWT_SECRET`
- `ALIS_SEED_ADMIN_EMAIL`, `ALIS_SEED_ADMIN_PASSWORD`
- `ALIS_CORS_ALLOWED_ORIGIN_PATTERNS` (set to your real frontend URL — do **not** leave `*` in production)

Render injects `PORT` automatically; the health check uses `/health`.

---

## API Documentation

- **OpenAPI JSON:** `alis-api.json` (committed to the repo)
- **Swagger UI (dev only):** `http://localhost:8080/swagger-ui.html`
- **Frontend integration guide:** `LOVABLE_API_BRIEF.md`
- **JS client wrapper:** `frontend-api/alisApi.js`

In production (`prod` profile) Swagger is disabled by default.

---

## Testing

```bash
./mvnw test
```

Test profile (`application-test.properties`) uses H2 in-memory.

---

## Security Notes

- JWT secret **must** be a high-entropy random string; never commit it.
- Tighten `ALIS_CORS_ALLOWED_ORIGIN_PATTERNS` to your real frontend domain(s) before going public.
- `spring.jpa.hibernate.ddl-auto` is `update` in dev and `validate`-friendly in prod — use Flyway/Liquibase if you need versioned migrations.
- Firebase service-account JSON is highly sensitive — prefer Render Secret Files or env injection over committing the file.
- `logging.level.org.springframework.security=DEBUG` in base config — consider lowering to `INFO`/`WARN` in production.

---

## License

No license file is present in the repository at the time of this scan. Add a `LICENSE` file (e.g. MIT, Apache-2.0) to clarify usage rights.

---

*Generated by scanning every file in the repository: 140 files across controllers, services, entities, DTOs, projections, queries, security, WebSocket, utilities, tests, deployment manifests, and frontend helpers.*
