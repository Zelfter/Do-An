-- RAG Schema migration 002: Question vectors for RAG retrieval
-- FR-15: retrieve questions by RAG per JD topic
-- FR-33-questions-index: question embedding indexed when published (AC-19)
-- design.md §4.1: embedding dim matches model output (default 768; configurable)
-- `public` kept in search_path so the pgvector `vector` type resolves.
SET search_path TO rag, public;

CREATE TABLE IF NOT EXISTS rag_question_vectors (
    question_id UUID PRIMARY KEY,
    -- Vector dimension matches settings.embedding_dim (default 768)
    -- SRS §10.5: "dung lượng ước tính khoảng 2.000 vector 768 chiều chiếm cỡ 6 MB"
    embedding vector(768),
    content_hash VARCHAR(64) NOT NULL,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Note: SRS §10.5 — at this scale (~2000 vectors) no vector index needed yet.
-- Add ivfflat/hnsw index when question count grows beyond 10K.

COMMENT ON TABLE rag.rag_question_vectors IS
    'Logical link to app.questions via question_id (no FK across schemas — SRS §10.5)';
COMMENT ON COLUMN rag.rag_question_vectors.content_hash IS
    'SHA-256 of indexed content — detect if re-index is needed when question is updated';
