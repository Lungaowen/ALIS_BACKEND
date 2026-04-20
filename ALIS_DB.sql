-- =============================================
-- LEGAL AI - PostgreSQL Schema (Corrected)
-- =============================================

-- 1. Enums
CREATE TYPE role_type AS ENUM ('ADMIN', 'ATTORNEY', 'PARALEGAL', 'USER');
CREATE TYPE risk_level AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE document_status AS ENUM ('ACTIVE', 'PROCESSING', 'ARCHIVED', 'ERROR');
CREATE TYPE ingestion_source AS ENUM ('MANUAL', 'WATCHED_FOLDER', 'API');
CREATE TYPE action_type AS ENUM (
    'LOGIN', 'LOGOUT', 'UPLOAD_DOCUMENT', 'DOCUMENT_DELETE',
    'DOCUMENT_ARCHIVE', 'ANALYSIS_RUN', 'SIMILARITY_RUN',
    'COMPLIANCE_CHECK', 'USER_CREATED', 'USER_UPDATED',
    'USER_DELETED', 'SYSTEM_ERROR', 'REPORT_GENERATED'
);
CREATE TYPE analysis_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED');

-- 2. Main Tables

-- Admins
CREATE TABLE admins (
    admin_id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Clients (Base table with inheritance support)
CREATE TABLE client (
    client_id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role role_type NOT NULL DEFAULT 'USER',
    username VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Subclasses (Joined Table Inheritance)
CREATE TABLE dealmaker (
    client_id INT PRIMARY KEY REFERENCES client(client_id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    deal_specialty TEXT
);

CREATE TABLE legal_practitioner (
    client_id INT PRIMARY KEY REFERENCES client(client_id) ON DELETE CASCADE,
    bar_number VARCHAR(100),
    law_firm VARCHAR(255)
);

-- Acts
CREATE TABLE act (
    act_id SERIAL PRIMARY KEY,
    act_name VARCHAR(255) NOT NULL UNIQUE,
    act_number VARCHAR(100),
    act_year SMALLINT,
    act_section VARCHAR(100),
    description TEXT,
    jurisdiction VARCHAR(100) DEFAULT 'South Africa'
);

-- Documents
CREATE TABLE document (
    document_id SERIAL PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(client_id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status document_status NOT NULL DEFAULT 'ACTIVE',
    ingestion_source ingestion_source NOT NULL DEFAULT 'MANUAL',
    archived_at TIMESTAMP
);

-- File Metadata
CREATE TABLE file_metadata (
    file_id SERIAL PRIMARY KEY,
    document_id INT NOT NULL UNIQUE REFERENCES document(document_id) ON DELETE CASCADE,
    mime_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    hash VARCHAR(255) NOT NULL UNIQUE,
    storage_path TEXT NOT NULL
);

-- Document Content (Extracted text + embedding)
CREATE TABLE document_content (
    content_id SERIAL PRIMARY KEY,
    document_id INT NOT NULL UNIQUE REFERENCES document(document_id) ON DELETE CASCADE,
    extracted_text TEXT,
    embedding_vector TEXT
);

-- Clauses
CREATE TABLE clause (
    clause_id SERIAL PRIMARY KEY,
    document_id INT NOT NULL REFERENCES document(document_id) ON DELETE CASCADE,
    clause_text TEXT NOT NULL,
    risk_level risk_level NOT NULL,
    risk_reason TEXT,
    page_number INT
);

-- Law Rules
CREATE TABLE law_rule (
    rule_id SERIAL PRIMARY KEY,
    act_id INT NOT NULL REFERENCES act(act_id) ON DELETE CASCADE,
    keyword VARCHAR(255) NOT NULL,
    requirements TEXT,
    risk_level risk_level NOT NULL DEFAULT 'MEDIUM',
    suggestion TEXT,
    edited BOOLEAN DEFAULT FALSE
);

-- Summary Reports
CREATE TABLE summary_report (
    report_id SERIAL PRIMARY KEY,
    document_id INT NOT NULL REFERENCES document(document_id) ON DELETE CASCADE,
    client_id INT NOT NULL REFERENCES client(client_id) ON DELETE CASCADE,
    rule_id INT NOT NULL REFERENCES law_rule(rule_id) ON DELETE CASCADE,
    
    similarity_score NUMERIC(5,2),
    risk_level risk_level NOT NULL,
    ai_recommendation TEXT,
    ai_explanation TEXT,
    analysis_status analysis_status DEFAULT 'PENDING',
    
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model_version VARCHAR(50)
);

-- Audit Logs
CREATE TABLE audit_log (
    log_id SERIAL PRIMARY KEY,
    admin_id INT REFERENCES admins(admin_id) ON DELETE SET NULL,
    client_id INT REFERENCES client(client_id) ON DELETE SET NULL,
    document_id INT REFERENCES document(document_id) ON DELETE SET NULL,
    
    action_type action_type NOT NULL,
    description TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- Indexes for Performance
-- =============================================

CREATE INDEX idx_client_email ON client(email);
CREATE INDEX idx_document_client ON document(client_id);
CREATE INDEX idx_audit_created ON audit_log(created_at DESC);
CREATE INDEX idx_audit_client ON audit_log(client_id);
CREATE INDEX idx_audit_action ON audit_log(action_type);
CREATE INDEX idx_summary_report_document ON summary_report(document_id);
CREATE INDEX idx_clause_document ON clause(document_id);

-- =============================================
-- Sample Data (Optional)
-- =============================================

-- INSERT INTO act (act_name, act_number, act_year, description) 
-- VALUES ('Consumer Protection Act', '68', 2008, 'Main consumer protection legislation in South Africa');