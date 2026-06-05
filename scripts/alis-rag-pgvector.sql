-- =============================================
-- ALIS RAG Setup for Supabase/PostgreSQL
-- =============================================
-- Run this in Supabase SQL Editor after the base ALIS schema exists.
-- It creates chunk storage, PostgreSQL full-text search indexes, and a pgvector
-- column that can be used later with OpenAI/OCI embeddings.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunk (
    chunk_id BIGSERIAL PRIMARY KEY,
    document_id INT NOT NULL REFERENCES document(document_id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    token_count INTEGER,
    embedding VECTOR(1536),
    search_vector tsvector,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_document_chunk_document_index
        UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunk_document
    ON document_chunk(document_id);

CREATE INDEX IF NOT EXISTS idx_document_chunk_fts
    ON document_chunk USING GIN(search_vector);

-- Use this index after embeddings are populated. Keep vector dimensions aligned
-- with the embedding model you choose.
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
    ON document_chunk USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE OR REPLACE FUNCTION alis_document_chunk_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', coalesce(NEW.chunk_text, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_document_chunk_fts ON document_chunk;
CREATE TRIGGER trg_document_chunk_fts
BEFORE INSERT OR UPDATE ON document_chunk
FOR EACH ROW EXECUTE FUNCTION alis_document_chunk_search_vector_update();

UPDATE document_chunk
SET search_vector = to_tsvector('english', coalesce(chunk_text, ''))
WHERE search_vector IS NULL;

-- Optional vector retrieval shape once your app stores embeddings:
--
-- SELECT
--     dc.chunk_id,
--     dc.document_id,
--     dc.chunk_text,
--     1 - (dc.embedding <=> CAST(:query_embedding AS vector)) AS similarity
-- FROM document_chunk dc
-- JOIN document d ON d.document_id = dc.document_id
-- WHERE dc.embedding IS NOT NULL
--   AND (:client_id IS NULL OR d.client_id = :client_id)
-- ORDER BY dc.embedding <=> CAST(:query_embedding AS vector)
-- LIMIT 5;
