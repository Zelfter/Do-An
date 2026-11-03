-- Migration V006: AI Call Logs
-- NFR-24: log every AI call (purpose, provider, model, latency, error)
-- AC-LOG-01: every /ai/v1/index/* call must produce a log entry
-- Used by FR-35 (Could): admin usage statistics

CREATE TABLE ai_call_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID,
    turn_id UUID,
    purpose VARCHAR(50) NOT NULL,
    provider VARCHAR(50),
    model VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT')),
    latency_ms INT,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_call_logs_session_id ON ai_call_logs(session_id);
CREATE INDEX idx_ai_call_logs_purpose ON ai_call_logs(purpose);
CREATE INDEX idx_ai_call_logs_status ON ai_call_logs(status);
CREATE INDEX idx_ai_call_logs_created_at ON ai_call_logs(created_at);

COMMENT ON TABLE ai_call_logs IS 'NFR-24, AC-LOG-01: immutable audit log for every Java→Python/LLM call';
COMMENT ON COLUMN ai_call_logs.purpose IS 'e.g. INDEX_QUESTION, INDEX_DOCUMENT, EVALUATE_TURN, JD_ANALYZE, GENERATE_REPORT';
COMMENT ON COLUMN ai_call_logs.session_id IS 'NULL for content management calls (indexing); set during interview session';
COMMENT ON COLUMN ai_call_logs.latency_ms IS 'Wall-clock time from Java perspective including network round-trip';
