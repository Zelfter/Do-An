-- Migration V007: Interview Session Tables
-- FR-09 (create session), FR-10 (config), FR-11 (timer), FR-14 (early end)
-- FR-22 (no question repeat), FR-23 (save transcript)
-- BR-09 (session end conditions), BR-16 (session state machine)
-- BR-17 (answer immutable), BR-18 (UNGRADED)
-- These tables support Spec 02/03; created here so schema is complete for Task 02.

-- Job Descriptions
CREATE TABLE job_descriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    raw_text TEXT NOT NULL,
    level VARCHAR(20) CHECK (level IN ('JUNIOR', 'MID', 'SENIOR')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ANALYZING', 'READY', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jd_user_id ON job_descriptions(user_id);

-- JD-to-topic mappings (result of JD analysis FR-06)
CREATE TABLE jd_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jd_id UUID NOT NULL REFERENCES job_descriptions(id) ON DELETE CASCADE,
    topic_id UUID REFERENCES topics(id) ON DELETE SET NULL,
    raw_term VARCHAR(300) NOT NULL,
    importance VARCHAR(20) CHECK (importance IN ('REQUIRED', 'PREFERRED', 'NICE_TO_HAVE')),
    is_selected BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_jd_topics_jd_id ON jd_topics(jd_id);
CREATE INDEX idx_jd_topics_topic_id ON jd_topics(topic_id);

-- Interview Sessions (FR-09, BR-16)
CREATE TABLE interview_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jd_id UUID NOT NULL REFERENCES job_descriptions(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'TIMED_OUT', 'CANCELLED')),
    time_limit_sec INT NOT NULL DEFAULT 3600,
    elapsed_ms BIGINT NOT NULL DEFAULT 0,
    config_snapshot JSONB NOT NULL DEFAULT '{}',
    total_score NUMERIC(5,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user_id ON interview_sessions(user_id);
CREATE INDEX idx_sessions_status ON interview_sessions(status);
COMMENT ON COLUMN interview_sessions.config_snapshot IS 'BR-01, NFR-29: snapshot of scoring_settings at session creation time — immutable once captured';

-- Session topic weights and scores (BR-10, BR-11, BR-14)
CREATE TABLE session_topics (
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    weight NUMERIC(4,2) NOT NULL DEFAULT 1.0,
    topic_score NUMERIC(5,2),
    is_weak BOOLEAN,
    PRIMARY KEY (session_id, topic_id)
);

-- Question pool for a session (FR-15: computed once at session creation)
CREATE TABLE session_question_pool (
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    rank INT NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (session_id, question_id)
);

CREATE INDEX idx_sqp_session_id ON session_question_pool(session_id);
COMMENT ON TABLE session_question_pool IS 'FR-15: RAG pool computed once at creation — no re-embedding during chat turns';

-- Interview turns (FR-16, FR-22, FR-23, BR-17)
CREATE TABLE session_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    parent_turn_id UUID REFERENCES session_turns(id) ON DELETE RESTRICT,
    question_id UUID REFERENCES questions(id) ON DELETE RESTRICT,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('ORIGINAL', 'FOLLOW_UP')),
    question_text TEXT NOT NULL,
    answer_text TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ANSWERED', 'SKIPPED', 'TIMED_OUT', 'UNGRADED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_turns_session_id ON session_turns(session_id);
CREATE INDEX idx_turns_parent_turn_id ON session_turns(parent_turn_id);
COMMENT ON COLUMN session_turns.parent_turn_id IS 'NULL for ORIGINAL turns; points to parent for FOLLOW_UP — BR-05, FR-20';
COMMENT ON COLUMN session_turns.answer_text IS 'BR-17: immutable once submitted; no UPDATE allowed on answered turns';

-- Turn evaluations (FR-17, BR-01)
CREATE TABLE turn_evaluations (
    turn_id UUID PRIMARY KEY REFERENCES session_turns(id) ON DELETE CASCADE,
    accuracy NUMERIC(4,2) NOT NULL,
    completeness NUMERIC(4,2) NOT NULL,
    clarity NUMERIC(4,2) NOT NULL,
    example_score NUMERIC(4,2) NOT NULL,
    total_score NUMERIC(5,2) NOT NULL,
    missed_key_points JSONB NOT NULL DEFAULT '[]',
    feedback TEXT,
    graded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE turn_evaluations IS 'BR-01: criteria stored separately; Java calculates total_score — never LLM';
COMMENT ON COLUMN turn_evaluations.feedback IS 'FR-26: generated when report is created (GENERATING→READY); NULL until then';

-- Session reports (FR-43, FR-25, FR-26)
CREATE TABLE session_reports (
    session_id UUID PRIMARY KEY REFERENCES interview_sessions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING'
        CHECK (status IN ('GENERATING', 'READY', 'PARTIAL')),
    summary TEXT,
    generated_at TIMESTAMPTZ
);

COMMENT ON TABLE session_reports IS 'FR-43: async report — GENERATING→READY or PARTIAL (AC-22)';
COMMENT ON COLUMN session_reports.status IS 'PARTIAL: scores and missed_key_points available even if LLM prose failed (AC-22)';
