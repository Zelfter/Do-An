-- Migration V005: Scoring Settings
-- NFR-29: thresholds, weights, limits stored in config — not hard-coded
-- BR-01: rubric weights 50/30/10/10
-- BR-03: score thresholds (Good ≥70%, Medium 40-69%, Weak <40%)
-- BR-05: follow-up threshold ≥70%, max 2 consecutive follow-ups
-- BR-14: weak topic threshold <50%
-- SRS §2.4: chốt tạm, tinh chỉnh khi test

CREATE TABLE scoring_settings (
    key VARCHAR(100) PRIMARY KEY,
    value JSONB NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE scoring_settings IS 'NFR-29: all thresholds and weights — never hard-coded in application code';
COMMENT ON COLUMN scoring_settings.key IS 'Setting key, e.g. rubric_weights, follow_up_threshold';
COMMENT ON COLUMN scoring_settings.value IS 'JSONB value — numeric, array or object depending on key';
