-- Migration V011: server-side timer pause/resume (NFR-16).
-- resume_at = wall-clock time when the current answering window started.
-- Null while the AI is grading (timer paused) or after the session has ended.
-- Enables server-authoritative remaining time and resume-after-refresh support.
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS resume_at TIMESTAMPTZ;
COMMENT ON COLUMN interview_sessions.resume_at IS 'NFR-16: wall-clock time when the current answering window started; null while AI grades or after session end';
