# API — Phỏng vấn mô phỏng (Candidate Interview)

> Phần 2/3 · FR-06/07/09/10/14/15/16/17/20/22/23/24/25/26/43
> BR-01/05/12/14/15/16/17/18/20 · NFR-16/29

## Tổng quan

- Các endpoint dành cho **ứng viên** (candidate): dán JD → phân tích → tạo phiên → phòng phỏng vấn → báo cáo.
- Frontend **chỉ** gọi Java `/api/v1` (architecture-guardrails). Java sở hữu state máy, timer, chấm điểm, follow-up eligibility, báo cáo. Python chỉ cung cấp JD terms / gợi ý tiêu chí / văn bản follow-up / tóm tắt báo cáo qua `AiServiceClient` (mặc định `MockAiServiceClient` — deterministic, không gọi LLM thật).
- **BR-20 / AC-05 / AC-12:** khi phiên `IN_PROGRESS`, mọi response cho ứng viên **KHÔNG** chứa điểm/criteria/feedback. Điểm chỉ có ở `GET /sessions/{id}/report`, sau khi phiên kết thúc.
- **BR-15 (ownership):** ứng viên chỉ truy cập JD/session/report của chính mình → vi phạm trả **403**; không tồn tại → **404**.
- Tất cả route dưới `/api/v1/**` → yêu cầu JWT hợp lệ (quy rule `authenticated`). Không cần đổi `SecurityConfig`.

## RBAC

| Endpoint | Quyền | Lỗi |
|---|---|---|
| `POST /api/v1/jd/analyze` | authenticated | Không JWT → 401 |
| `POST /api/v1/sessions` | authenticated | Không JWT → 401 |
| `GET /api/v1/sessions` | authenticated | Không JWT → 401 |
| `GET /api/v1/sessions/{id}/state` | authenticated + owner | 404/403 |
| `POST /api/v1/sessions/{id}/start` | authenticated + owner | 404/403 |
| `POST /api/v1/sessions/{id}/turns/{turnId}/answer` | authenticated + owner | 404/403 |
| `POST /api/v1/sessions/{id}/turns/{turnId}/skip` | authenticated + owner | 404/403 |
| `POST /api/v1/sessions/{id}/end` | authenticated + owner | 404/403 |
| `GET /api/v1/sessions/{id}/report` | authenticated + owner + đã kết thúc | 404/403/409 |

RoleHierarchy `ADMIN > CANDIDATE`.

## State machine (BR-16)

```
CREATED --start--> IN_PROGRESS --end|pool hết|timeout--> COMPLETED|TIMED_OUT
CREATED --end--> CANCELLED
```

- `start`: CREATED → IN_PROGRESS, hỏi câu đầu (FR-16). Chỉ CREATED mới start được (khác → 409).
- `answer`/`skip`: idempotent (turn không còn `PENDING` → trả state hiện tại, không double-submit — BR-18).
- `end` (FR-14): IN_PROGRESS → COMPLETED (finalize + báo cáo); CREATED → CANCELLED.
- Timeout (NFR-16): server-authoritative; khi `remainingSec <= 0` → TIMED_OUT, chấm câu đang `PENDING` = 0 (BR-12), tạo báo cáo.

## Timer (NFR-16)

- `interview_sessions.elapsed_ms` + `resume_at`. `remainingSec = timeLimitSec - elapsedMs - (now - resumeAt)` khi đang ở lượt chờ trả lời.
- Timer **tạm dừng khi AI đang chấm** (resume_at = null giữa submit và câu kế tiếp), tiếp tục khi câu kế tiếp được trình bày.

## Endpoint

### POST /api/v1/jd/analyze  (FR-06)
Body: `{ "rawText": "..." }` → `201` `{ jdId, level, terms:[{jdTopicId, rawTerm, topicId, topicCode, importance, selected}] }`.
- `topicId` null = thuật ngữ chưa map được (FR-38). Mặc định `selected=true`. Lỗi AI → `422`.

### POST /api/v1/sessions  (FR-09, FR-15)
Body: `{ jdId, level, timeLimitSec, selectedTopicIds:[] }` → `201` `SessionState` (status `CREATED`).
- Pool câu hỏi được tính **một lần** lúc tạo từ câu `PUBLISHED+INDEXED` của chủ đề đã chọn, lưu `session_question_pool`. Không re-embed mỗi lượt.
- Không có câu hỏi phù hợp → `400`. `timeLimitSec` 60–7200.

### GET /api/v1/sessions  (FR-24)
→ `200` `[{ sessionId, status, level, totalScore (null nếu chưa kết thúc), createdAt }]`.

### GET /api/v1/sessions/{id}/state  (BR-20)
→ `200` `SessionState` (KHÔNG chứa điểm khi IN_PROGRESS): `{ sessionId, status, level, timeLimitSec, elapsedMs, remainingSec, topics, totalQuestions, answeredCount, followUpCount, turns:[{turnId, kind, questionText, answerText, topicName, topicCode}], currentTurn|null, endedAt }`.

### POST /api/v1/sessions/{id}/start
→ `200` `SessionState` (IN_PROGRESS, `currentTurn` = câu đầu).

### POST /api/v1/sessions/{id}/turns/{turnId}/answer  (FR-17, BR-17, BR-18)
Body: `{ requestId, answerText }` → `200` `SessionState`.
- `requestId` idempotent; answer bất biến. Trả về câu kế tiếp (follow-up hoặc câu gốc kế) hoặc trạng thái kết thúc.
- AI thất bại sau retry → turn `UNGRADED` (BR-18), **không** coi là 0.

### POST /api/v1/sessions/{id}/turns/{turnId}/skip  (BR-12)
Body: `{ requestId }` → `200` `SessionState`. Bỏ qua = 0.

### POST /api/v1/sessions/{id}/end  (FR-14)
→ `200` `{ sessionId, status, endedAt }`. Finalize + tạo báo cáo.

### GET /api/v1/sessions/{id}/report  (FR-25/26/43, BR-20)
Chỉ khi phiên đã kết thúc (khác → `409`). → `200` `SessionReport`:
`{ sessionId, status, reportStatus (GENERATING|READY|PARTIAL), level, totalScore, classification, topics:[{topicId,code,name,topicScore,isWeak}], weakTopics, turns:[{turnId,kind,questionText,answerText,status,accuracy,completeness,clarity,exampleScore,totalScore,missedKeyPoints,feedback}], summary, studyMaterials:[{topicId,topicName,documentId,title}], generatedAt }`.
- `PARTIAL` (AC-22): vẫn giữ điểm/chủ đề yếu dù AI sinh văn bản tóm tắt thất bại (`summary` null).

## Scoring (BR-01)

- 4 tiêu chí 0–10: `accuracy`(0.50) · `completeness`(0.30) · `clarity`(0.10) · `example`(0.10). **Java tính tổng** (không phải LLM).
- Tổng 0–10; `pct = total*10`. Phân loại từ `total_score_classification` (excellent≥80, good≥65, average≥50, else needs_improvement).
- Chủ đề yếu (BR-14): `topic_score% < weak_topic_threshold(50)` HOẶC `≥ min_weak_answers(2)`.
- Follow-up (BR-05): xem `docs/decisions/interview-follow-up-threshold.md`.
- Tất cả ngưỡng/trọng số đọc từ `scoring_settings` + snapshot mỗi phiên (NFR-29).

## Ví dụ

```http
POST /api/v1/sessions/{id}/turns/{turnId}/answer
Authorization: Bearer <jwt>
Content-Type: application/json
{ "requestId": "8c1f...-uuid", "answerText": "Dependency injection là..." }
```
Thành công → `SessionState` với `currentTurn` là câu kế tiếp. Hết pool → `status: "COMPLETED"`, `endedAt` set.
Lỗi → `{ timestamp, status, message }` (ErrorResponse).
