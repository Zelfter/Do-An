# API — Python AI Service (Internal)

> Task 07 · M3 · FR-33 · FR-33-questions-index · NFR-24 · AC-LOG-01 · AC-DOC-01 · AC-DOC-02
> AC-Q-03

## Tổng quan

- Các endpoint **nội bộ** của Python AI Service, chạy tại `AI_SERVICE_BASE_URL` (cấu hình qua env).
- Chỉ **Java Backend** được phép gọi — không expose trực tiếp ra internet.
- Tất cả route `/ai/v1/index/*` yêu cầu header `X-Internal-Key` khớp với env `INTERNAL_API_KEY` (design.md §6).
- Mỗi lời gọi `/ai/v1/index/*` đều ghi log vào `ai_call_logs` với `purpose`, `provider`, `model`, `latency_ms`, `status` (NFR-24, AC-LOG-01).

## Phân quyền (Internal Key Auth)

| Endpoint                              | Xác thực                | Không thỏa →                         |
|---------------------------------------|-------------------------|--------------------------------------|
| `GET /ai/v1/health`                   | Không yêu cầu           | N/A                                  |
| `POST /ai/v1/index/questions`         | `X-Internal-Key` header | 401 (thiếu hoặc sai key)             |
| `DELETE /ai/v1/index/questions/{id}`  | `X-Internal-Key` header | 401 (thiếu hoặc sai key)             |
| `POST /ai/v1/index/documents`         | `X-Internal-Key` header | 401 (thiếu hoặc sai key)             |
| `DELETE /ai/v1/index/documents/{id}`  | `X-Internal-Key` header | 401 (thiếu hoặc sai key)             |

**Cách cấu hình:**
- Python đọc key từ env `INTERNAL_API_KEY`.
- Java gửi key qua header `X-Internal-Key: <INTERNAL_API_KEY>`.
- Hai giá trị phải khớp nhau (cùng được cấu hình từ cùng một secret).

---

## Mô hình dữ liệu (Data models)

### IndexQuestionRequest

```json
{
  "questionId": "a1b2...uuid",
  "topicCode": "oop",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP và cho ví dụ thực tế?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc: Encapsulation, Inheritance, Polymorphism, Abstraction...",
  "keyPoints": [
    "Encapsulation: che giấu trạng thái nội bộ qua getter/setter",
    "Inheritance: tái sử dụng code qua kế thừa",
    "Polymorphism: một interface, nhiều cách thực thi",
    "Abstraction: ẩn chi tiết, chỉ lộ giao diện cần thiết"
  ]
}
```

| Trường            | Kiểu        | Bắt buộc | Mô tả                                                               |
|-------------------|-------------|----------|---------------------------------------------------------------------|
| `questionId`      | UUID        | Có       | ID câu hỏi trong schema `app`.                                      |
| `topicCode`       | string      | Có       | Code snake_case của chủ đề (VD: `oop`, `core_java`).                |
| `language`        | string      | Không    | Mã ngôn ngữ. Mặc định `vi`.                                         |
| `level`           | enum        | Có       | Cấp độ: `JUNIOR`, `MID`, hoặc `SENIOR`.                             |
| `content`         | string      | Có       | Nội dung câu hỏi.                                                   |
| `referenceAnswer` | string      | Có       | Đáp án chuẩn. Cùng `content` tạo thành đơn vị embedding duy nhất.  |
| `keyPoints`       | string[]    | Không    | Danh sách ý chính (plain string, không phải object).                |

### IndexQuestionResponse

```json
{
  "indexed": true,
  "contentHash": "e3b0c44298fc1c149afb...64-char-hex"
}
```

| Trường        | Kiểu    | Mô tả                                                                                            |
|---------------|---------|--------------------------------------------------------------------------------------------------|
| `indexed`     | boolean | `true` nếu embedding đã được upsert vào `rag_question_vectors`.                                 |
| `contentHash` | string  | SHA-256 (hex, 64 ký tự) của chuỗi `content + "\n" + referenceAnswer` — dùng để detect thay đổi. |

### IndexDocumentRequest

```json
{
  "documentId": "a1b2...uuid",
  "topicCode": "oop",
  "language": "vi",
  "title": "Tổng quan lập trình hướng đối tượng trong Java",
  "contentMd": "## Encapsulation\n\nEncapsulation là nguyên tắc...\n\n## Inheritance\n\n..."
}
```

| Trường       | Kiểu   | Bắt buộc | Mô tả                                   |
|--------------|--------|----------|-----------------------------------------|
| `documentId` | UUID   | Có       | ID tài liệu trong schema `app`.          |
| `topicCode`  | string | Có       | Code snake_case của chủ đề.             |
| `language`   | string | Không    | Mã ngôn ngữ. Mặc định `vi`.             |
| `title`      | string | Có       | Tiêu đề tài liệu.                        |
| `contentMd`  | string | Có       | Nội dung tài liệu định dạng Markdown.   |

### IndexDocumentResponse

```json
{
  "chunkCount": 3
}
```

| Trường       | Kiểu | Mô tả                                                     |
|--------------|------|-----------------------------------------------------------|
| `chunkCount` | int  | Số chunk đã được tạo và upsert vào `rag_document_chunks`. |

### Error response

```json
{ "detail": "Unauthorized: missing or invalid X-Internal-Key" }
```

---

## GET /ai/v1/health

Kiểm tra trạng thái hoạt động của AI Service. Không yêu cầu xác thực.

**Response 200:**
```json
{
  "status": "ok",
  "service": "ai-service",
  "db": "ok",
  "embedding_provider": "mock"
}
```

| Trường               | Kiểu   | Giá trị có thể             | Mô tả                                          |
|----------------------|--------|---------------------------|------------------------------------------------|
| `status`             | enum   | `ok` \| `degraded`         | Trạng thái tổng thể của service.               |
| `service`            | string | `"ai-service"`             | Tên service.                                   |
| `db`                 | enum   | `ok` \| `unavailable`      | Trạng thái kết nối đến Neon PostgreSQL.        |
| `embedding_provider` | enum   | `mock` \| `gemini` \| `groq` | Provider embedding đang được cấu hình.       |

> `status = "degraded"` khi `db` hoặc `embedding_provider` không khả dụng; Java nên log cảnh báo nhưng không block luồng chính.

---

## POST /ai/v1/index/questions

FR-33-questions-index, AC-Q-03: Lập chỉ mục vector cho một câu hỏi. Java gọi endpoint này ngay sau khi publish câu hỏi thành công.

**Header:**
```
X-Internal-Key: <INTERNAL_API_KEY>
```

**Request:**
```json
{
  "questionId": "a1b2...uuid",
  "topicCode": "oop",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP và cho ví dụ thực tế?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc: Encapsulation, Inheritance, Polymorphism, Abstraction...",
  "keyPoints": [
    "Encapsulation: che giấu trạng thái nội bộ qua getter/setter",
    "Inheritance: tái sử dụng code qua kế thừa"
  ]
}
```

**Chiến lược embedding (design.md §4.4):**

Python tạo embedding cho chuỗi ghép:
```
{content}\n{referenceAnswer}
```
như một đơn vị duy nhất và upsert vào `rag_question_vectors`. `keyPoints` không tham gia trực tiếp vào vector nhưng được lưu kèm metadata để retrieval.

**Response 200:**
```json
{
  "indexed": true,
  "contentHash": "e3b0c44298fc1c149afb4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

**Response 401** (thiếu hoặc sai `X-Internal-Key`):
```json
{ "detail": "Unauthorized: missing or invalid X-Internal-Key" }
```

**Response 500** (lỗi phía Python — embedding provider không khả dụng, lỗi DB):
```json
{ "detail": "Indexing failed: <mô tả lỗi>" }
```

**Hành vi phía Java khi nhận response:**

```
POST /ai/v1/index/questions
  ├─ 200 → update indexStatus = INDEXED
  │         log AiCallLog(SUCCESS)
  └─ 4xx/5xx/timeout (40s)
       → update indexStatus = INDEX_FAILED
         log AiCallLog(FAILED, error=<detail>)
         vẫn trả 200 cho Admin với indexStatus = INDEX_FAILED
```

**Python retry nội bộ:** 2 lần với exponential backoff trước khi trả lỗi 500.

---

## DELETE /ai/v1/index/questions/{question_id}

Xóa embedding của câu hỏi khỏi `rag_question_vectors`. Java gọi khi Admin xóa câu hỏi đã được indexed.

**Header:**
```
X-Internal-Key: <INTERNAL_API_KEY>
```

**Path param:**

| Tham số      | Kiểu | Mô tả                                |
|--------------|------|--------------------------------------|
| `question_id`| UUID | ID câu hỏi cần xóa khỏi vector store. |

**Response 204:**
> Không có body. Trả về khi xóa thành công (hoặc bản ghi không tồn tại — idempotent).

**Response 401** (thiếu hoặc sai `X-Internal-Key`):
```json
{ "detail": "Unauthorized: missing or invalid X-Internal-Key" }
```

**Response 500** (lỗi phía Python):
```json
{ "detail": "Delete failed: <mô tả lỗi>" }
```

---

## POST /ai/v1/index/documents

FR-33, AC-DOC-01: Lập chỉ mục vector cho một tài liệu học tập. Java gọi endpoint này khi Admin tạo mới hoặc reindex tài liệu.

**Header:**
```
X-Internal-Key: <INTERNAL_API_KEY>
```

**Request:**
```json
{
  "documentId": "a1b2...uuid",
  "topicCode": "oop",
  "language": "vi",
  "title": "Tổng quan lập trình hướng đối tượng trong Java",
  "contentMd": "## Encapsulation\n\nEncapsulation là nguyên tắc che giấu trạng thái nội bộ...\n\n## Inheritance\n\nInheritance cho phép tái sử dụng code..."
}
```

**Chiến lược chunking (design.md §4.4):**

Python phân tách tài liệu Markdown thành các chunk theo quy tắc:
- Ranh giới chunk: heading `##` (H2) hoặc `###` (H3).
- Kích thước tối đa mỗi chunk: **512 token**.
- Overlap giữa các chunk liền kề: **64 token**.
- Mỗi chunk được embed độc lập và upsert vào `rag_document_chunks`.

Ví dụ với tài liệu 3 section H2 → `chunkCount = 3`.

**Response 200:**
```json
{
  "chunkCount": 3
}
```

**Response 401** (thiếu hoặc sai `X-Internal-Key`):
```json
{ "detail": "Unauthorized: missing or invalid X-Internal-Key" }
```

**Response 500** (lỗi phía Python — embedding provider không khả dụng, lỗi DB):
```json
{ "detail": "Indexing failed: <mô tả lỗi>" }
```

**Hành vi phía Java khi nhận response (AC-DOC-01, AC-DOC-02):**

```
POST /ai/v1/index/documents
  ├─ 200 → update document.status = INDEXED
  │         log AiCallLog(SUCCESS, purpose=INDEX_DOCUMENT)
  └─ 4xx/5xx/timeout (40s)
       → update document.status = INDEX_FAILED
         log AiCallLog(FAILED, error=<detail>)
         vẫn trả 201 cho Admin với indexStatus = INDEX_FAILED
         (không throw exception ra ngoài — AC-DOC-02)
```

---

## DELETE /ai/v1/index/documents/{document_id}

Xóa tất cả các chunk của tài liệu khỏi `rag_document_chunks`. Java gọi khi Admin xóa tài liệu.

**Header:**
```
X-Internal-Key: <INTERNAL_API_KEY>
```

**Path param:**

| Tham số       | Kiểu | Mô tả                                     |
|---------------|------|-------------------------------------------|
| `document_id` | UUID | ID tài liệu cần xóa toàn bộ chunk khỏi vector store. |

**Response 204:**
> Không có body. Trả về khi xóa thành công (idempotent — trả về 204 kể cả khi không có chunk nào tồn tại).

**Response 401** (thiếu hoặc sai `X-Internal-Key`):
```json
{ "detail": "Unauthorized: missing or invalid X-Internal-Key" }
```

**Response 500** (lỗi phía Python):
```json
{ "detail": "Delete failed: <mô tả lỗi>" }
```

---

## Logging (NFR-24, AC-LOG-01)

Mỗi lời gọi `/ai/v1/index/*` (cả thành công lẫn thất bại) đều ghi một bản ghi vào bảng `ai_call_logs` trong schema `app`:

```json
{
  "purpose": "INDEX_QUESTION",
  "provider": "gemini",
  "model": "text-embedding-004",
  "status": "SUCCESS",
  "latency_ms": 312,
  "error": null
}
```

| Trường       | Kiểu   | Giá trị có thể                                            | Mô tả                                          |
|--------------|--------|----------------------------------------------------------|------------------------------------------------|
| `purpose`    | string | `INDEX_QUESTION` \| `INDEX_DOCUMENT` \| `DELETE_QUESTION` \| `DELETE_DOCUMENT` | Loại thao tác. |
| `provider`   | string | `mock` \| `gemini` \| `groq`                             | Embedding provider được dùng.                 |
| `model`      | string | Tên model embedding                                       | Model cụ thể (từ env `EMBEDDING_MODEL`).       |
| `status`     | enum   | `SUCCESS` \| `FAILED` \| `TIMEOUT`                       | Kết quả lời gọi.                              |
| `latency_ms` | int    | ≥ 0                                                      | Thời gian xử lý (milliseconds).               |
| `error`      | string | null hoặc mô tả lỗi                                      | Chỉ có giá trị khi `status ≠ SUCCESS`.        |

> Log ghi từ phía **Java** sau khi nhận response từ Python. Python log cục bộ ở mức INFO với JSON structured format để trace nội bộ.

---

## Schema database (sở hữu bởi Python)

Python quản lý migration riêng cho schema `rag` qua SQL script khi khởi động:

```sql
-- rag_question_vectors
question_id    UUID PRIMARY KEY,
embedding      VECTOR(768),          -- chiều phụ thuộc model
content_hash   VARCHAR(64),          -- SHA-256 hex để detect thay đổi
indexed_at     TIMESTAMPTZ

-- rag_document_chunks
id             UUID PRIMARY KEY,
document_id    UUID NOT NULL,
chunk_index    INT NOT NULL,
chunk_text     TEXT NOT NULL,
embedding      VECTOR(768),
indexed_at     TIMESTAMPTZ,
UNIQUE(document_id, chunk_index)
```

> Không có foreign key xuyên schema giữa `app` và `rag` — liên kết là logic thông qua `question_id` / `document_id` (SRS §10.5).

---

## Cấu hình môi trường

**Python (`.env`):**

| Biến                  | Mô tả                                              | Ví dụ            |
|-----------------------|----------------------------------------------------|------------------|
| `DATABASE_URL`        | Chuỗi kết nối Neon PostgreSQL                      | `postgresql://...` |
| `INTERNAL_API_KEY`    | Key xác thực nội bộ Java↔Python (phải khớp Java)  | `<secret>`       |
| `EMBEDDING_PROVIDER`  | Provider tạo embedding                             | `gemini`         |
| `EMBEDDING_MODEL`     | Tên model embedding                                | `text-embedding-004` |
| `GEMINI_API_KEY`      | API key cho Gemini (nếu dùng Gemini provider)      | `<secret>`       |
| `GROQ_API_KEY`        | API key cho Groq (nếu dùng Groq provider)          | `<secret>`       |

**Môi trường test / CI:** đặt `EMBEDDING_PROVIDER=mock` để dùng mock provider, không gọi API thật.
