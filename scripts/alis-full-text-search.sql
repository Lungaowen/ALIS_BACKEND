-- =============================================
-- ALIS Full-Text Search Setup for Supabase/PostgreSQL
-- =============================================
-- Run this once in the Supabase SQL Editor.
-- It is safe to re-run: columns/indexes/functions are idempotent and triggers
-- are recreated after being dropped if they already exist.

-- 1. Add tsvector columns.
ALTER TABLE document
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

ALTER TABLE document_content
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

ALTER TABLE summary_report
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

ALTER TABLE clause
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- 2. GIN indexes for fast full-text lookup.
CREATE INDEX IF NOT EXISTS idx_document_fts
    ON document USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_document_content_fts
    ON document_content USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_summary_report_fts
    ON summary_report USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_clause_fts
    ON clause USING GIN (search_vector);

-- 3. Populate existing rows.
UPDATE document
SET search_vector = to_tsvector(
    'english',
    regexp_replace(coalesce(title, ''), '[^[:alnum:]]+', ' ', 'g') || ' ' ||
        coalesce(CAST(status AS text), '')
);

UPDATE document_content
SET search_vector = to_tsvector('english', coalesce(extracted_text, ''));

UPDATE summary_report
SET search_vector = to_tsvector(
    'english',
    coalesce(ai_recommendation, '') || ' ' ||
    coalesce(ai_explanation, '') || ' ' ||
    coalesce(CAST(risk_level AS text), '')
);

UPDATE clause
SET search_vector = to_tsvector(
    'english',
    coalesce(clause_text, '') || ' ' ||
    coalesce(risk_reason, '')
);

-- 4. Auto-update triggers keep vectors fresh on INSERT/UPDATE.
CREATE OR REPLACE FUNCTION alis_document_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'english',
        regexp_replace(coalesce(NEW.title, ''), '[^[:alnum:]]+', ' ', 'g') || ' ' ||
            coalesce(CAST(NEW.status AS text), '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_document_fts ON document;
CREATE TRIGGER trg_document_fts
BEFORE INSERT OR UPDATE ON document
FOR EACH ROW EXECUTE FUNCTION alis_document_search_vector_update();

CREATE OR REPLACE FUNCTION alis_document_content_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', coalesce(NEW.extracted_text, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_document_content_fts ON document_content;
CREATE TRIGGER trg_document_content_fts
BEFORE INSERT OR UPDATE ON document_content
FOR EACH ROW EXECUTE FUNCTION alis_document_content_search_vector_update();

CREATE OR REPLACE FUNCTION alis_summary_report_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'english',
        coalesce(NEW.ai_recommendation, '') || ' ' ||
        coalesce(NEW.ai_explanation, '') || ' ' ||
        coalesce(CAST(NEW.risk_level AS text), '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_summary_report_fts ON summary_report;
CREATE TRIGGER trg_summary_report_fts
BEFORE INSERT OR UPDATE ON summary_report
FOR EACH ROW EXECUTE FUNCTION alis_summary_report_search_vector_update();

CREATE OR REPLACE FUNCTION alis_clause_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'english',
        coalesce(NEW.clause_text, '') || ' ' ||
        coalesce(NEW.risk_reason, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_clause_fts ON clause;
CREATE TRIGGER trg_clause_fts
BEFORE INSERT OR UPDATE ON clause
FOR EACH ROW EXECUTE FUNCTION alis_clause_search_vector_update();

-- 5. Unified document/content view for ad-hoc SQL inspection.
CREATE OR REPLACE VIEW document_search_view AS
SELECT
    d.document_id,
    d.client_id,
    d.title,
    d.status,
    d.uploaded_at,
    dc.extracted_text,
    coalesce(d.search_vector, CAST('' AS tsvector)) ||
        coalesce(dc.search_vector, CAST('' AS tsvector)) AS combined_vector
FROM document d
LEFT JOIN document_content dc ON dc.document_id = d.document_id;
