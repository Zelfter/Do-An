---
inclusion: always
---
# Non-negotiable boundaries and rules
- Frontend talks ONLY to Java public `/api/v1`; never to AI provider, Python service or database and never holds API keys.
- Java owns JWT/roles, business state/time, topic selection, scoring calculation (BR-01), transitions, follow-up eligibility (BR-05), limits, aggregate scores, reporting state and schema `app`. No direct LLM/embedding calls, no vector writes.
- Python owns chunking, embeddings, RAG, JD extraction, rubric criterion suggestions, follow-up text, report prose, provider adapter, validated structured JSON and schema `rag`. Python does NOT own interview session state or make final business-rule decisions.
- BR-20, AC-05, AC-12: while interview is ongoing, NO candidate-facing response (including `/sessions/{id}/state` and answer response) may expose score, criterion values or evaluation feedback. Report readable only after session ends. Explicitly test serialization and authorization.
- BR-01 rubric: accuracy 50%, completeness 30%, clarity 10%, examples 10%; each criterion 0..10; Java calculates total. Thresholds in configurable settings, initial follow-up score >=70%, maximum 2 consecutive follow-ups, weak topic <50% or >=2 weak answers (BR-14).
- Question pool retrieved once at creation and persisted, used for fallback; don't re-embed during each chat turn. Answer submission uses `requestId` for idempotency; accept only active session; timer pauses while AI works. Retry/backoff 429/503, fallback provider then UNGRADED (BR-18). Do not treat UNGRADED as zero. Skipped/unanswered/time-out = 0 according to BR-12.
- Async report: GENERATING -> READY or PARTIAL; preserve numeric scores, missed points and available study references on AI generation failure (AC-22).
- Protect ownership (BR-15), RBAC, prompt injection (JD and answer are untrusted DATA). Validate LLM JSON and bounds; use internal key + request ID on internal API if design retained.
- Mark assumptions explicitly; run design checkpoint before applying changes that resolve ambiguous SRS details.
