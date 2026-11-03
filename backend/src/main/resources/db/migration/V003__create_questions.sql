-- Migration V003: Questions and Key Points
-- FR-32: Admin CRUD questions with reference_answer and key_points
-- BR-02: every question must have reference_answer + key_points for scoring
-- BR-19: publish requires topic, level, content, reference_answer, ≥1 key_point
-- AC-18 (≡ AC-Q-01): publish without required fields → HTTP 422
-- AC-19 (≡ AC-Q-02): DRAFT questions must not appear in RAG pool

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    language VARCHAR(8) NOT NULL DEFAULT 'vi',
    level VARCHAR(20) NOT NULL CHECK (level IN ('JUNIOR', 'MID', 'SENIOR')),
    content TEXT NOT NULL,
    reference_answer TEXT NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    index_status VARCHAR(20) NOT NULL DEFAULT 'NOT_INDEXED'
        CHECK (index_status IN ('NOT_INDEXED', 'INDEXING', 'INDEXED', 'INDEX_FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_topic_id ON questions(topic_id);
CREATE INDEX idx_questions_status ON questions(status);
CREATE INDEX idx_questions_index_status ON questions(index_status);
CREATE INDEX idx_questions_level ON questions(level);
CREATE INDEX idx_questions_language ON questions(language);

-- Composite index for RAG pool query (FR-15): only PUBLISHED + INDEXED questions
CREATE INDEX idx_questions_rag_pool ON questions(topic_id, level, status, index_status)
    WHERE status = 'PUBLISHED' AND index_status = 'INDEXED';

COMMENT ON TABLE questions IS 'FR-32: Question bank — DRAFT questions excluded from RAG pool (AC-19)';
COMMENT ON COLUMN questions.reference_answer IS 'BR-02: required for scoring; empty only in DRAFT';
COMMENT ON COLUMN questions.status IS 'DRAFT→PUBLISHED requires all mandatory fields (BR-19, AC-18)';
COMMENT ON COLUMN questions.index_status IS 'NOT_INDEXED|INDEXING|INDEXED|INDEX_FAILED — INDEX_FAILED blocks RAG selection';

-- Key points (ý chính) for each question (BR-02, BR-19, FR-32)
CREATE TABLE question_key_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    seq INT NOT NULL,
    content TEXT NOT NULL,
    UNIQUE (question_id, seq)
);

CREATE INDEX idx_qkp_question_id ON question_key_points(question_id);

COMMENT ON TABLE question_key_points IS 'BR-02, BR-19: ≥1 key_point required before question can be published';
COMMENT ON COLUMN question_key_points.seq IS 'Display order of key points; starts at 1';
