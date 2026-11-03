# Spec 01 — Foundation & Content Management · Design

> **Trạng thái:** Chờ phê duyệt — không implement trước khi `requirements.md` được phê duyệt
> **Phụ thuộc:** `requirements.md` (Spec 01) · SRS §3.1, §3.7, §4, §5, §10.1–10.5
> **Cập nhật:** 2026-10-16

---

## 1. Kiến trúc tổng thể của spec này

Spec 01 chạm vào ba thành phần chính theo sơ đồ SRS §10.1:

```
Browser (React/Vite)
     │  REST /api/v1  Bearer JWT
     ▼
Java Backend (Spring Boot)          ← sở hữu schema `app`
  M1 Identity & Access
  M2 Content Management
     │  REST /ai/v1  X-Internal-Key
     ▼
Python AI Service (FastAPI)         ← sở hữu schema `rag`
  M3 Indexing & Retrieval (RAG)
     │
     ▼
Neon PostgreSQL  (pgvector)
  schema app  +  schema rag
```

**Nguyên tắc bất biến (từ architecture-guardrails.md):**
- Frontend không bao giờ gọi trực tiếp Python Service hoặc LLM API.
- Java không bao giờ gọi LLM hoặc tạo vector.
- Python không lưu trạng thái nghiệp vụ; không quyết định business rule.
- Mọi quyết định ủy quyền (auth) nằm ở Java.

---

## 2. Thiết kế module M1 — Identity & Access

### 2.1 Domain model

```
User {
  id          UUID PK
  email       VARCHAR(254) UNIQUE NOT NULL
  password_hash  VARCHAR NOT NULL        -- bcrypt cost≥12 hoặc argon2id
  role        ENUM(CANDIDATE, ADMIN) NOT NULL
  status      ENUM(ACTIVE, LOCKED) NOT NULL DEFAULT ACTIVE
  created_at  TIMESTAMPTZ
  updated_at  TIMESTAMPTZ
}

RefreshToken {
  id          UUID PK
  user_id     UUID FK→users.id
  token_hash  VARCHAR NOT NULL UNIQUE   -- SHA-256 của raw token
  expires_at  TIMESTAMPTZ NOT NULL
  revoked     BOOLEAN NOT NULL DEFAULT FALSE
  created_at  TIMESTAMPTZ
}
```

> **Quyết định thiết kế (D-03):** Spec này đề xuất có refresh token lưu server-side để hỗ trợ:
> - Revoke ngay khi Admin khóa user (FR-34, AC-USER-01).
> - Tránh phiên phỏng vấn bị mất do access token hết hạn (NFR-16, spec sau).
>
> ⚠️ Cần chủ dự án xác nhận trước khi code.

### 2.2 API endpoints — Auth

```
POST   /api/v1/auth/register         → 201 | 409
POST   /api/v1/auth/login            → 200 {access_token, refresh_token, expires_in}
POST   /api/v1/auth/refresh          → 200 {access_token, expires_in} | 401
POST   /api/v1/auth/logout           → 204  (client xóa token)
GET    /api/v1/me                    → 200 {id, email, role, status}
PATCH  /api/v1/me                    → 200 (Should — FR-02, không thuộc MVP Must)
```

**JWT payload (access token):**
```json
{
  "sub": "<user-uuid>",
  "role": "CANDIDATE",
  "iat": 1700000000,
  "exp": 1700003600
}
```

**Bảo mật:**
- `password_hash`: bcrypt cost=12 (mặc định Spring Security) hoặc argon2id.
- Access token TTL: lấy từ env `JWT_ACCESS_TTL_MINUTES` (default 60).
- Refresh token TTL: lấy từ env `JWT_REFRESH_TTL_DAYS` (default 7).
- Refresh token lưu dưới dạng `SHA-256(raw_token)` trong DB; raw token chỉ trả về client một lần.
- Khi user bị LOCKED: revoke tất cả RefreshToken của user đó.

### 2.3 API endpoints — Admin Users

```
GET    /api/v1/admin/users                    → 200 [{id, email, role, status, ...}]
GET    /api/v1/admin/users/{id}               → 200 | 404
PATCH  /api/v1/admin/users/{id}/status        → 200 | 400 (self-lock) | 404
```

### 2.4 Spring Security filter chain

```
SecurityFilterChain:
  - JwtAuthFilter (OncePerRequestFilter): extract + validate JWT
  - RoleHierarchy: ADMIN > CANDIDATE
  - Matcher rules:
      /api/v1/auth/**           → permitAll
      /api/v1/admin/**          → hasRole(ADMIN)
      /api/v1/**                → authenticated
      /ai/v1/**                 → internal key check (không qua JWT)
      /api/v1/system/warmup     → permitAll
```

### 2.5 Package structure — Java (M1)

```
backend/src/main/java/
  com.example.interview/
    domain/
      user/
        User.java
        UserRole.java
        UserStatus.java
        RefreshToken.java
    application/
      auth/
        AuthService.java
        TokenService.java
        UserManagementService.java
      port/
        UserRepository.java
        RefreshTokenRepository.java
    infrastructure/
      persistence/
        UserJpaEntity.java
        RefreshTokenJpaEntity.java
        UserJpaRepository.java
        RefreshTokenJpaRepository.java
      security/
        JwtAuthFilter.java
        JwtProperties.java
    api/
      auth/
        AuthController.java
        AuthRequest.java
        AuthResponse.java
      admin/
        AdminUserController.java
        UserStatusRequest.java
```

---

## 3. Thiết kế module M2 — Content Management

### 3.1 Domain model (schema `app`)

```
topics {
  id           UUID PK
  code         VARCHAR(64) UNIQUE NOT NULL   -- snake_case, immutable
  parent_id    UUID FK→topics.id NULLABLE
  is_active    BOOLEAN NOT NULL DEFAULT TRUE
  created_at   TIMESTAMPTZ
}

topic_translations {
  topic_id     UUID FK→topics.id
  language     VARCHAR(8) NOT NULL           -- 'vi', 'en'
  name         VARCHAR(200) NOT NULL
  description  TEXT
  PRIMARY KEY (topic_id, language)
}

topic_aliases {
  id       UUID PK
  topic_id UUID FK→topics.id
  alias    VARCHAR(200) NOT NULL
  UNIQUE(topic_id, alias)
}

questions {
  id               UUID PK
  topic_id         UUID FK→topics.id NOT NULL
  language         VARCHAR(8) NOT NULL DEFAULT 'vi'
  level            ENUM(JUNIOR, MID, SENIOR) NOT NULL
  content          TEXT NOT NULL
  reference_answer TEXT NOT NULL
  status           ENUM(DRAFT, PUBLISHED, ARCHIVED) NOT NULL DEFAULT DRAFT
  index_status     ENUM(NOT_INDEXED, INDEXING, INDEXED, INDEX_FAILED) DEFAULT NOT_INDEXED
  created_at       TIMESTAMPTZ
  updated_at       TIMESTAMPTZ
}

question_key_points {
  id          UUID PK
  question_id UUID FK→questions.id NOT NULL
  seq         INT NOT NULL
  content     TEXT NOT NULL
  UNIQUE(question_id, seq)
}

documents {
  id           UUID PK
  topic_id     UUID FK→topics.id NOT NULL
  language     VARCHAR(8) NOT NULL DEFAULT 'vi'
  title        VARCHAR(400) NOT NULL
  content_md   TEXT NOT NULL
  status       ENUM(DRAFT, INDEXED, INDEX_FAILED) NOT NULL DEFAULT DRAFT
  index_error  TEXT
  created_at   TIMESTAMPTZ
  updated_at   TIMESTAMPTZ
}

scoring_settings {
  key          VARCHAR(100) PK
  value        JSONB NOT NULL
  updated_by   UUID FK→users.id
  updated_at   TIMESTAMPTZ
}
```

### 3.2 API endpoints — Admin Topics

```
GET    /api/v1/admin/topics              → 200 [TopicDto]
POST   /api/v1/admin/topics              → 201 TopicDto | 409 (code duplicate)
GET    /api/v1/admin/topics/{id}         → 200 TopicDto | 404
PUT    /api/v1/admin/topics/{id}         → 200 TopicDto
PATCH  /api/v1/admin/topics/{id}         → 200 (partial update incl. is_active)
DELETE /api/v1/admin/topics/{id}         → 204 | 409 (has linked questions/docs)

GET    /api/v1/admin/topics/{id}/aliases  → 200 [AliasDto]
POST   /api/v1/admin/topics/{id}/aliases  → 201 AliasDto
DELETE /api/v1/admin/topics/{id}/aliases/{aliasId} → 204
```

### 3.3 API endpoints — Admin Questions

```
GET    /api/v1/admin/questions                       → 200 [QuestionSummaryDto] (paginated)
POST   /api/v1/admin/questions                       → 201 QuestionDto
GET    /api/v1/admin/questions/{id}                  → 200 QuestionDetailDto | 404
PUT    /api/v1/admin/questions/{id}                  → 200 QuestionDto
DELETE /api/v1/admin/questions/{id}                  → 204 | 409 (used in session)

POST   /api/v1/admin/questions/{id}/publish          → 200 | 422 {missingFields:[...]}
POST   /api/v1/admin/questions/{id}/archive          → 200
POST   /api/v1/admin/questions/{id}/reindex          → 202 {indexJobId}

POST   /api/v1/admin/questions/import                → 200 {total, success, failed, errors:[{row,msg}]}
```

### 3.4 API endpoints — Admin Documents (Study Materials)

```
GET    /api/v1/admin/documents                       → 200 [DocumentSummaryDto] (paginated)
POST   /api/v1/admin/documents                       → 201 DocumentDto  (+auto-trigger indexing)
GET    /api/v1/admin/documents/{id}                  → 200 DocumentDto | 404
PUT    /api/v1/admin/documents/{id}                  → 200 DocumentDto
DELETE /api/v1/admin/documents/{id}                  → 204 | 409 (if INDEXED and referenced)

POST   /api/v1/admin/documents/{id}/reindex          → 202 {indexJobId}
```

### 3.5 Package structure — Java (M2)

```
backend/src/main/java/
  com.example.interview/
    domain/
      topic/
        Topic.java
        TopicTranslation.java
        TopicAlias.java
      question/
        Question.java
        QuestionLevel.java
        QuestionStatus.java
        QuestionKeyPoint.java
        IndexStatus.java
      document/
        StudyDocument.java
        DocumentStatus.java
      scoring/
        ScoringSettings.java
    application/
      topic/
        TopicService.java
      question/
        QuestionService.java
        QuestionImportService.java
      document/
        StudyDocumentService.java
        IndexingOrchestrator.java      -- gọi Python AI Service
      port/
        TopicRepository.java
        QuestionRepository.java
        StudyDocumentRepository.java
        AiServiceClient.java           -- interface
        AiCallLogRepository.java
    infrastructure/
      persistence/                     -- JPA entities
      http/
        PythonAiServiceClient.java     -- implements AiServiceClient
        AiServiceProperties.java
      logging/
        AiCallLogService.java
    api/
      admin/
        AdminTopicController.java
        AdminQuestionController.java
        AdminDocumentController.java
        AdminScoringController.java
```

---

## 4. Thiết kế module M3 — RAG Indexing (Python AI Service)

### 4.1 Schema `rag` (sở hữu bởi Python)

```sql
-- Python quản lý migration riêng (Alembic hoặc SQL script)
rag_question_vectors (
  question_id    UUID PRIMARY KEY,
  embedding      VECTOR(768),           -- chiều tùy model
  content_hash   VARCHAR(64),           -- SHA-256 để detect thay đổi
  indexed_at     TIMESTAMPTZ
);

rag_document_chunks (
  id             UUID PRIMARY KEY,
  document_id    UUID NOT NULL,
  chunk_index    INT NOT NULL,
  chunk_text     TEXT NOT NULL,
  embedding      VECTOR(768),
  indexed_at     TIMESTAMPTZ,
  UNIQUE(document_id, chunk_index)
);
```

Liên kết giữa `questions`/`documents` (schema `app`) và `rag_*` (schema `rag`) là **logic** — không có foreign key xuyên schema (theo SRS §10.5).

### 4.2 API endpoints — Python Internal (`/ai/v1`)

```
POST /ai/v1/index/questions          → {indexed: bool, contentHash: str}
DELETE /ai/v1/index/questions/{id}   → 204

POST /ai/v1/index/documents          → {chunkCount: int}
DELETE /ai/v1/index/documents/{id}   → 204

GET  /ai/v1/health                   → {status, provider, db}
```

**POST /ai/v1/index/questions — request body:**
```json
{
  "questionId": "uuid",
  "topicCode": "oop",
  "language": "vi",
  "level": "JUNIOR",
  "content": "...",
  "referenceAnswer": "...",
  "keyPoints": ["...", "..."]
}
```

**POST /ai/v1/index/documents — request body:**
```json
{
  "documentId": "uuid",
  "topicCode": "oop",
  "language": "vi",
  "title": "...",
  "contentMd": "..."
}
```

### 4.3 Package structure — Python (M3, phần index)

```
ai-service/
  api/
    routes/
      index.py          -- POST /index/questions, /index/documents
      health.py
    schemas/
      index_schemas.py
  services/
    indexing_service.py       -- orchestrate chunk + embed + upsert
    chunking_service.py       -- markdown-aware text splitter
    embedding_service.py      -- gọi embedding API
  providers/
    base_provider.py          -- abstract
    gemini_provider.py
    groq_provider.py
    mock_provider.py          -- dùng trong test, không gọi API thật
  rag/
    vector_store.py           -- pgvector operations
  core/
    config.py
    logging.py                -- ai_call_logs
    db.py
```

### 4.4 Chunking strategy

- Tài liệu Markdown: chunk theo heading H2/H3 làm ranh giới; max chunk 512 token; overlap 64 token.
- Câu hỏi: embed `content + "\n" + reference_answer` như một đơn vị duy nhất.
- Model embedding: do NFR-28 yêu cầu kiểm thử, dùng env `EMBEDDING_MODEL` và `EMBEDDING_PROVIDER` để cấu hình; không hard-code tên model.

### 4.5 Xử lý lỗi indexing

```
Java gọi POST /ai/v1/index/*
  ├── 200 OK → Java cập nhật index_status = INDEXED
  ├── 4xx/5xx (timeout, rate-limit, provider down)
  │     → Java cập nhật index_status = INDEX_FAILED
  │     → Java log AiCallLog với error details
  │     → Không throw exception ra Admin (AC-DOC-02)
  └── Python retry nội bộ: 2 lần với exponential backoff trước khi trả lỗi
```

---

## 5. Cơ sở dữ liệu — Migrations

### 5.1 Flyway (Java / schema `app`)

```
db/migration/
  V001__create_users.sql
  V002__create_topics.sql
  V003__create_questions.sql
  V004__create_documents.sql
  V005__create_scoring_settings.sql
  V006__create_ai_call_logs.sql
  V007__create_refresh_tokens.sql
  V008__seed_default_topics.sql      -- 8 chủ đề mặc định + aliases
  V009__seed_scoring_settings.sql    -- ngưỡng mặc định
  V010__seed_admin_user.sql          -- đọc từ env ADMIN_EMAIL, ADMIN_PASSWORD
```

### 5.2 Alembic hoặc SQL script (Python / schema `rag`)

```
ai-service/migrations/
  001_create_rag_schema.sql
  002_create_rag_question_vectors.sql
  003_create_rag_document_chunks.sql
```

> Python chạy migration khi khởi động (`startup` event) hoặc qua lệnh tách biệt.

---

## 6. Bảo mật

| Điểm | Quyết định thiết kế |
|---|---|
| Password hashing | Spring Security `BCryptPasswordEncoder` (cost 12) |
| JWT signing | HMAC-SHA256 (HS256); secret từ env `JWT_SECRET` (≥32 bytes); cân nhắc RS256 nếu cần multi-service verify |
| Internal API auth | Header `X-Internal-Key` giữa Java↔Python; giá trị từ env `INTERNAL_API_KEY` |
| SQL injection | Spring Data JPA / parameterized queries; không concatenate string |
| Input validation | Bean Validation (`@Valid`) trên tất cả request body; giới hạn content_md ≤100KB, question content ≤5000 chars |
| CORS | Chỉ allow origin của frontend (env `ALLOWED_ORIGINS`); không `*` |

---

## 7. Logging và observability

- Mỗi lời gọi Java→Python ghi một bản ghi vào bảng `ai_call_logs` (NFR-24):

```
ai_call_logs {
  id          UUID PK
  session_id  UUID NULLABLE
  turn_id     UUID NULLABLE
  purpose     VARCHAR(50)      -- 'INDEX_QUESTION', 'INDEX_DOCUMENT', ...
  provider    VARCHAR(50)
  model       VARCHAR(100)
  status      ENUM(SUCCESS, FAILED, TIMEOUT)
  latency_ms  INT
  error       TEXT NULLABLE
  created_at  TIMESTAMPTZ
}
```

- Java log ở mức INFO cho các thao tác Admin; WARN cho lỗi indexing; ERROR cho lỗi bất ngờ.
- Python log ở mức INFO với structured JSON để dễ trace.

---

## 8. Cấu hình môi trường

**Java (`application.yml` / env vars):**
```yaml
# Không commit giá trị thực, chỉ ví dụ key name
JWT_SECRET: <từ env>
JWT_ACCESS_TTL_MINUTES: 60
JWT_REFRESH_TTL_DAYS: 7
INTERNAL_API_KEY: <từ env>
AI_SERVICE_BASE_URL: <từ env>
AI_SERVICE_TIMEOUT_SECONDS: 40
ALLOWED_ORIGINS: <từ env>
ADMIN_EMAIL: <từ env>
ADMIN_PASSWORD: <từ env>
```

**Python (`.env` / env vars):**
```
DATABASE_URL=<neon connection string>
INTERNAL_API_KEY=<phải khớp Java>
EMBEDDING_PROVIDER=gemini   # hoặc groq / mock
EMBEDDING_MODEL=<từ env>
GEMINI_API_KEY=<từ env>
GROQ_API_KEY=<từ env>
```

---

## 9. Sequence diagrams (các luồng chính)

### 9.1 Admin publish câu hỏi + trigger indexing

```
Admin ──POST /admin/questions/{id}/publish──► Java
Java: validate đủ trường (BR-19)
  ├─ thiếu → 422 {missingFields}
  └─ đủ → update status=PUBLISHED
         → gọi POST /ai/v1/index/questions  (async hoặc sync)
              ├─ 200 → update index_status=INDEXED → 200 OK cho Admin
              └─ lỗi → update index_status=INDEX_FAILED
                      → log AiCallLog(FAILED)
                      → vẫn trả 200 cho Admin với warning flag
```

### 9.2 Admin lưu tài liệu mới + trigger indexing

```
Admin ──POST /admin/documents──► Java
Java: validate, lưu document (status=DRAFT)
Java ──POST /ai/v1/index/documents──► Python
Python: chunk → embed → upsert rag_document_chunks
  ├─ OK → 200 {chunkCount}
  │       Java: update status=INDEXED → 201 cho Admin
  └─ lỗi → Java: update status=INDEX_FAILED, log error
           → vẫn trả 201 cho Admin với index_status=INDEX_FAILED
```

### 9.3 Đăng nhập

```
Client ──POST /auth/login {email, password}──► Java
Java: load user, verify bcrypt hash
  ├─ sai → 401
  ├─ LOCKED → 403
  └─ OK → tạo access_token (JWT), tạo refresh_token
         → lưu hash(refresh_token) vào DB
         → 200 {access_token, refresh_token, expires_in}
```

---

## 10. Phụ thuộc kỹ thuật đề xuất

**Java:**
- Spring Boot 3.x, Spring Security 6.x, Spring Data JPA
- `jjwt` (io.jsonwebtoken) cho JWT
- `spring-validation` (Bean Validation)
- Flyway cho migration
- Dự kiến không dùng thêm thư viện nặng để giữ heap trong 256–320 MB

**Python:**
- FastAPI, uvicorn
- `httpx` cho HTTP calls
- `asyncpg` hoặc `psycopg[async]` cho Neon
- `pgvector` Python client
- Không có `torch`, `sentence-transformers`, `transformers`

---

## 11. Quyết định thiết kế còn mở

| # | Quyết định | Ảnh hưởng | Mặc định đề xuất |
|---|---|---|---|
| D-01 | React/Vite hay Vue? | FE structure | React/Vite (steering/tech.md) |
| D-02 | MVP có xác minh email không? | FR-01, M1 | Không xác minh (đơn giản hơn) |
| D-03 | JWT stateless hay có refresh token? | NFR-17, NFR-16 | Có refresh token (design này đã chọn) |
| D-IndexSync | Indexing khi publish câu hỏi: sync hay async? | UX, độ tin cậy | Sync với timeout 40s; nếu timeout → INDEX_FAILED |

> Tất cả quyết định trên cần chủ dự án xác nhận trước khi code M1.

---

*Tài liệu này chờ phê duyệt. Sau khi được phê duyệt, tasks.md sẽ hướng dẫn implementation từng bước.*
