-- RAG Schema migration 003: Document chunks for study material retrieval
-- FR-33: documents chunked and embedded for RAG (design.md §4.4)
-- FR-28: weak topics link to study materials; retrieved via embedding similarity
-- `public` kept in search_path so the pgvector `vector` type resolves.
SET search_path TO rag, public;

CREATE TABLE IF NOT EXISTS rag_document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_rag_doc_chunks_doc_id
    ON rag.rag_document_chunks(document_id);

COMMENT ON TABLE rag.rag_document_chunks IS
    'FR-33: Markdown documents chunked by H2/H3 headings — design.md §4.4';
COMMENT ON COLUMN rag.rag_document_chunks.chunk_text IS
    'Max 512 tokens per chunk; overlap 64 tokens — settings.chunk_max_tokens';
