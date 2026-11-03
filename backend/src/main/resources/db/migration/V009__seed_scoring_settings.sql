-- Migration V009: Default Scoring Settings
-- NFR-29: thresholds never hard-coded — stored in scoring_settings
-- SRS §2.4: "các chỉ số nghiệp vụ tạm chốt, tinh chỉnh khi test"
-- BR-01: rubric weights 50/30/10/10
-- BR-03: Good ≥70%, Medium 40-69%, Weak <40%
-- BR-05: follow-up threshold ≥70%, max 2 consecutive follow-ups
-- BR-14: weak topic threshold <50%

INSERT INTO scoring_settings (key, value, updated_by, updated_at) VALUES

-- Rubric weights (BR-01): accuracy=50%, completeness=30%, clarity=10%, example=10%
-- ⚠️ SRS §2.4: "tinh chỉnh khi test"
('rubric_weights', '{
    "accuracy": 0.50,
    "completeness": 0.30,
    "clarity": 0.10,
    "example": 0.10
}', NULL, CURRENT_TIMESTAMP),

-- Score category thresholds (BR-03)
-- good_threshold: ≥70 → "Tốt"; medium_threshold: 40–69 → "Trung bình"; <40 → "Yếu"
('score_thresholds', '{
    "good": 70,
    "medium": 40,
    "weak": 0
}', NULL, CURRENT_TIMESTAMP),

-- Follow-up logic (BR-05)
-- trigger_threshold: answer score must be ≥70% to trigger follow-up
-- max_consecutive: maximum consecutive follow-ups per original question
('follow_up_config', '{
    "trigger_threshold": 70,
    "max_consecutive": 2
}', NULL, CURRENT_TIMESTAMP),

-- Weak topic threshold (BR-14)
-- topic_score_threshold: topic score <50% → weak
-- min_weak_answers: or ≥2 answers rated "Yếu" → weak
('weak_topic_config', '{
    "topic_score_threshold": 50,
    "min_weak_answers": 2
}', NULL, CURRENT_TIMESTAMP),

-- JD required skill weight multiplier (BR-11)
-- required_skill_weight: required skills in JD weighted ×1.5 in total score
('score_weights', '{
    "required_skill_multiplier": 1.5
}', NULL, CURRENT_TIMESTAMP),

-- Total score classification (BR-13)
-- excellent ≥80, good 65-79, average 50-64, needs_improvement <50
('total_score_classification', '{
    "excellent": 80,
    "good": 65,
    "average": 50,
    "needs_improvement": 0
}', NULL, CURRENT_TIMESTAMP),

-- API quota guard (NFR-09, NFR-32)
-- max_sessions_per_day_per_user: daily session limit per user
-- max_llm_calls_per_session: hard cap on LLM calls per session (SRS §2.4 ~15–20 calls)
('quota_config', '{
    "max_sessions_per_day_per_user": 5,
    "max_llm_calls_per_session": 20
}', NULL, CURRENT_TIMESTAMP);

-- 7 settings seeded — all configurable via Admin settings API (Task 06)
