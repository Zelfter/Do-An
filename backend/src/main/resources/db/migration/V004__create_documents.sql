-- Migration V004: Study Documents (tài liệu ôn tập)
-- FR-33: Admin manages study materials and triggers RAG indexing
-- FR-28: from weak topic → open corresponding study material
-- AC-DOC-01: INDEXED docs are retrievable; un-indexed docs don't appear in study materials
-- AC-DOC-02: indexing failure → document saved with INDEX_FAILED, no error thrown to Admin

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    language VARCHAR(8) NOT NULL DEFAULT 'vi',
    title VARCHAR(400) NOT NULL,
    content_md TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'INDEXED', 'INDEX_FAILED')),
    index_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_documents_topic_id ON documents(topic_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_language ON documents(language);

-- Index for study material retrieval: only INDEXED documents (AC-DOC-01)
CREATE INDEX idx_documents_study_materials ON documents(topic_id, language, status)
    WHERE status = 'INDEXED';

COMMENT ON TABLE documents IS 'FR-33, FR-28: study materials with embedding-based indexing into rag schema';
COMMENT ON COLUMN documents.content_md IS 'Markdown content; max 100KB enforced at application layer (design.md §6)';
COMMENT ON COLUMN documents.status IS 'DRAFT (not indexed) | INDEXED (retrievable) | INDEX_FAILED (indexing error)';
COMMENT ON COLUMN documents.index_error IS 'AC-DOC-02: error message stored when index_status=INDEX_FAILED';
