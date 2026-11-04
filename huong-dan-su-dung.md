# Hướng dẫn chạy và sử dụng hệ thống

> Hệ thống ôn Java & phỏng vấn mô phỏng theo JD — 3 dịch vụ (Java · Python · React) + Neon PostgreSQL
Cách chạy:
bash
bash start-dev.sh
Cách dừng:
bash stop.sh
---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Yêu cầu môi trường](#2-yêu-cầu-môi-trường)
3. [Thiết lập biến môi trường](#3-thiết-lập-biến-môi-trường)
4. [Thiết lập cơ sở dữ liệu](#4-thiết-lập-cơ-sở-dữ-liệu)
5. [Khởi động các dịch vụ](#5-khởi-động-các-dịch-vụ)
6. [Đăng nhập hệ thống](#6-đăng-nhập-hệ-thống)
7. [Sử dụng giao diện Admin](#7-sử-dụng-giao-diện-admin)
8. [Tham chiếu API](#8-tham-chiếu-api)
9. [Chạy kiểm thử](#9-chạy-kiểm-thử)
10. [Khắc phục sự cố thường gặp](#10-khắc-phục-sự-cố-thường-gặp)
11. [Triển khai production](#11-triển-khai-production)

---

## 1. Tổng quan kiến trúc

Hệ thống gồm 3 dịch vụ độc lập + 1 cơ sở dữ liệu dùng chung:

```
frontend/      React 18 + Vite → giao diện Admin (Tiếng Việt)
backend/       Spring Boot 3 · Java 21 → REST API, JWT auth, RBAC
ai-service/    FastAPI Python → RAG indexing (embedding, pgvector)
```

**Luồng dữ liệu:**

```
Browser → frontend(:5173)
  └─ proxy /api/v1 → backend(:8080)
       └─ REST /ai/v1 → ai-service(:8000)
            └─ Neon PostgreSQL (schema app + schema rag)
```

**Nguyên tắc bất biến:**

- Frontend **không bao giờ** gọi trực tiếp Python/LLM — tất cả qua Java
- Java **không tạo vector / gọi LLM** — chỉ orchestrate
- Python **không giữ trạng thái phiên** — stateless
- Giao diện **toàn bộ tiếng Việt** (NFR-26)

Chi tiết: `docs/architecture/overview.md`

---

## 2. Yêu cầu môi trường

| Công cụ | Phiên bản | Cách kiểm tra |
|---------|-----------|---------------|
| Java (JDK) | 21+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Python | 3.12+ | `python --version` |
| pip | đi kèm Python | `pip --version` |
| Node.js | 20+ | `node --version` |
| PostgreSQL | 15+ (hoặc Neon) | — |

Bạn cũng cần một tài khoản [Neon](https://neon.tech) (free tier) để lấy connection string PostgreSQL.

---

## 3. Thiết lập biến môi trường

### 3.1. Tạo file `.env`

Từ thư mục gốc dự án:

```bash
cp .env.example .env
```

### 3.2. Điền giá trị

Mở `.env` và điền các giá trị thực tế:

```bash
# --- Database (Neon PostgreSQL) ---
# Lấy từ Neon dashboard → project → connection string
# Định dạng postgresql:// (KHÔNG dùng postgresql+psycopg://)
DATABASE_URL=postgresql://user:password@ep-xxx.neon.tech/dbname?sslmode=require

# Credentials riêng cho Java (JDBC)
DB_USERNAME=your_neon_user
DB_PASSWORD=your_neon_password

# --- JWT (Java Backend) ---
# Tạo chuỗi ngẫu nhiên ≥32 ký tự: openssl rand -base64 48
JWT_SECRET=điền_ít_nhất_32_ký_tự_ngẫu_nhiên_vào_đây
JWT_ACCESS_TTL_MINUTES=60
JWT_REFRESH_TTL_DAYS=7

# --- Internal API key (Java ↔ Python) ---
# Hai dịch vụ phải dùng chung giá trị này
INTERNAL_API_KEY=dev-internal-key-change-me

# --- AI Service base URL (Java → Python) ---
AI_SERVICE_BASE_URL=http://localhost:8000
AI_SERVICE_TIMEOUT_SECONDS=40

# --- CORS ---
ALLOWED_ORIGINS=http://localhost:5173

# --- Admin seed (Flyway V010) ---
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=ChangeMe123!

# --- Embedding provider (Python AI Service) ---
# "mock" = dev/test, không gọi API thật, không tốn quota
EMBEDDING_PROVIDER=mock
EMBEDDING_MODEL=text-embedding-004
```

> ⚠️ **Không bao giờ commit file `.env` vào git.** File `.gitignore` đã loại trừ `.env`.

---

## 4. Thiết lập cơ sở dữ liệu

Hệ thống dùng **hai schema** trong cùng một Neon database:

| Schema | Sở hữu | Nội dung |
|--------|--------|---------|
| `app` | Java (Flyway V001–V010) | users, topics, questions, documents, ai_call_logs, interview_sessions... |
| `rag` | Python (migration 001–003) | rag_question_vectors, rag_document_chunks |

### 4.1. Tạo database & schema trên Neon

1. Đăng nhập Neon → tạo project mới
2. Tạo **dev branch** (tách khỏi main)
3. Copy connection string vào `DATABASE_URL` trong `.env`
4. Schema `app` và `rag` sẽ được tạo tự động bởi migrations

### 4.2. Chạy Flyway migrations (schema `app`)

```bash
cd backend
mvn flyway:migrate
```

Migrations V001–V010 chạy tự động:
- V001: users + refresh_tokens
- V002: topics + topic_translations + topic_aliases
- V003: questions + question_key_points
- V004: study_documents
- V005: scoring_settings
- V006: ai_call_logs
- V007: interview_tables (sessions, turns, evaluations, reports)
- V008: seed 8 chủ đề mặc định (OOP, Core Java, Collections, Java 8+, Concurrency, JVM, Spring Boot, Database & JPA)
- V009: seed scoring settings
- V010: seed admin user (email/password từ `ADMIN_EMAIL`/`ADMIN_PASSWORD`)

> **Lưu ý:** V010 dùng pgcrypto để bcrypt-hash mật khẩu admin. Đảm bảo extension pgvector đã bật trên Neon (Settings → Extensions).

### 4.3. Migrations schema `rag` (tự động khi khởi động Python)

Schema `rag` migrations (001–003) chạy **tự động khi khởi động ai-service** thông qua lifespan event. Không cần chạy thủ công.

Nếu muốn chạy thủ công:

```bash
cd ai-service
source .venv/bin/activate
python -m core.migrations
```

---

## 5. Khởi động các dịch vụ

Cần **3 terminal** — mỗi dịch vụ chạy trên một terminal riêng.

### 5.1. Java Backend (port 8080)

```bash
cd backend

# Lần đầu — tải dependencies (~2 phút):
mvn dependency:resolve -q

# Khởi động:
mvn spring-boot:run
```

Kiểm tra:

```bash
curl http://localhost:8080/api/v1/system/warmup
# {"status":"ok","service":"backend","timestamp":"..."}
```

> JVM options cho free tier (NFR-30): `-Xms64m -Xmx320m -XX:+UseSerialGC` — đã cấu hình trong `pom.xml`

### 5.2. Python AI Service (port 8000)

```bash
cd ai-service

# Tạo virtual environment (lần đầu):
python -m venv .venv

# Kích hoạt:
source .venv/bin/activate        # Linux/Mac
# .venv\Scripts\activate         # Windows

# Cài dependencies (lần đầu):
pip install -r requirements.txt

# Khởi động:
uvicorn main:app --reload --port 8000
```

Kiểm tra:

```bash
curl http://localhost:8000/ai/v1/health
# {"status":"ok"}
```

> Mặc định `EMBEDDING_PROVIDER=mock` — không gọi API thật, không tốn quota.
> Swagger docs chỉ mở khi `EMBEDDING_PROVIDER=mock`: http://localhost:8000/docs
> **Không dùng torch / sentence-transformers** (NFR-06, NFR-30).

### 5.3. Frontend (port 5173)

```bash
cd frontend

# Cài dependencies (lần đầu):
npm install

# Khởi động:
npm run dev
```

Mở trình duyệt: http://localhost:5173

> Vite proxy: `/api/v1` → `localhost:8080` (cấu hình trong `vite.config.ts`)

### 5.4. Thứ tự khởi động khuyến nghị

1. Database (Neon — luôn sẵn sàng)
2. Java Backend (chạy Flyway migrations)
3. Python AI Service (chạy rag migrations trên startup)
4. Frontend (gọi warmup endpoint khi tải trang)

---

## 6. Đăng nhập hệ thống

### 6.1. Tài khoản Admin mặc định

Sau khi Flyway V010 chạy xong, một tài khoản ADMIN được tạo với:

| Trường | Giá trị mặc định | Nguồn |
|--------|-------------------|-------|
| Email | `admin@example.com` | `ADMIN_EMAIL` env var |
| Mật khẩu | `ChangeMe123!` | `ADMIN_PASSWORD` env var |
| Role | ADMIN | — |
| Status | ACTIVE | — |

> Thay đổi email/mật khẩu qua biến môi trường `ADMIN_EMAIL` / `ADMIN_PASSWORD` **trước** khi chạy migration.

### 6.2. Đăng ký ứng viên mới

Endpoint `/api/v1/auth/register` công khai — người đăng ký mới luôn là `CANDIDATE` + `ACTIVE`.

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" 
  -d '{"email":"candidate@example.com","password":"Password123!"}'
```

### 6.3. Đăng nhập qua giao diện

1. Mở http://localhost:5173 → tự chuyển đến `/login`
2. Nhập email + mật khẩu (tài khoản Admin hoặc Candidate)
3. Hệ thống gọi `POST /api/v1/auth/login` → nhận `accessToken` + `refreshToken`
4. Token lưu trong `sessionStorage` (xoá khi đóng tab)
5. Admin → chuyển đến `/admin/topics`; Candidate → chuyển về `/login` (chưa có giao diện Candidate)

---

## 7. Sử dụng giao diện Admin

Truy cập: http://localhost:5173/admin (yêu cầu role ADMIN)

### 7.1. Bố cục

- **Header**: tên hệ thống, email người dùng + role, nút "Đăng xuất"
- **Menu ngang**: Chủ đề · Câu hỏi · Tài liệu · Người dùng
- **Khu vực nội dung**: bảng dữ liệu + form CRUD

### 7.2. Quản lý Chủ đề (Topics)

Trang `/admin/topics`:

| Thao tác | Mô tả |
|-----------|-------|
| Xem danh sách | Hiển thị tất cả chủ đề (8 chủ đề seed + chủ đề tự tạo) |
| Tạo chủ đề | Nhập `code` (snake_case, không đổi được sau khi tạo), translations, aliases |
| Sửa chủ đề | Đổi translations/aliases (code cố định) |
| Vô hiệu hóa | PATCH `isActive = false` — chủ đề ngưng hiển thị nhưng không xoá |
| Xoá chủ đề | Chỉ khi không có câu hỏi/tài liệu liên kết (nếu có → 409) |
| Quản lý aliases | Thêm/xoá alias phục vụ JD matching (ví dụ: "OOP", "HĐDT", "Đối tượng") |

8 chủ đề seed: OOP, Core Java, Collections, Java 8+ Lambda & Stream, Concurrency & Multithreading, JVM & Memory, Spring Boot, Database & JPA — mỗi chủ đề có translation tiếng Việt + tiếng Anh và aliases.

### 7.3. Quản lý Câu hỏi (Questions)

Trang `/admin/questions`:

| Thao tác | Mô tả |
|-----------|-------|
| Xem danh sách | Tất cả câu hỏi với trạng thái DRAFT / PUBLISHED / ARCHIVED |
| Tạo câu hỏi | Chọn chủ đề, level (JUNIOR/MID/SENIOR), nhập nội dung |
| Sửa câu hỏi | Cập nhật nội dung, reference answer, key points |
| Publish | Kiểm tra đủ điều kiện (reference answer + ≥1 key point) → trigger indexing sang Python |
| Archive | Gỡ câu hỏi khỏi RAG pool (xoá vector) |
| Xoá | Xoá hoàn toàn |

Vòng đời câu hỏi: `DRAFT → PUBLISHED → ARCHIVED`

> Khi publish thành công, Java tự động gọi `POST /ai/v1/index/questions` để tạo embedding (mock provider trong dev).

### 7.4. Quản lý Tài liệu (Documents)

Trang `/admin/documents`:

| Thao tác | Mô tả |
|-----------|-------|
| Xem danh sách | Tất cả tài liệu học (trạng thái INDEXED / INDEX_FAILED) |
| Tạo tài liệu | Chọn chủ đề, nhập tiêu đề + nội dung Markdown → tự động trigger indexing |
| Sửa tài liệu | Cập nhật nội dung (không tự reindex) |
| Reindex | Thủ công gọi lại indexing sau khi sửa nội dung |
| Xoá | Xoá tài liệu + chunks trong rag schema |

### 7.5. Quản lý Người dùng (Users)

Trang `/admin/users`:

| Thao tác | Mô tả |
|-----------|-------|
| Xem danh sách | Lọc theo role (ADMIN/CANDIDATE) hoặc status (ACTIVE/LOCKED) |
| Xem chi tiết | Thông tin user (không bao gồm password hash) |
| Khóa/Mở khóa | PATCH status → `ACTIVE` hoặc `LOCKED` |

**Quy tắc:**
- Khóa user → tất cả refresh token của user đó bị thu hồi (AC-USER-01)
- Admin không thể tự khóa tài khoản của mình → 400 (AC-USER-02)
- Mật khẩu **không bao giờ** xuất hiện trong response (NFR-17)

---

## 8. Tham chiếu API

### 8.1. Auth endpoints

Tất cả ở `/api/v1/auth/**` — công khai (không cần JWT).

| Method | Endpoint | Mô tả | Response |
|--------|----------|-------|----------|
| POST | `/auth/register` | Đăng ký (luôn CANDIDATE) | 201 `{id, email, role, status}` |
| POST | `/auth/login` | Đăng nhập | 200 `{accessToken, refreshToken, expiresIn}` |
| POST | `/auth/refresh` | Làm mới access token | 200 `{accessToken, expiresIn}` |
| POST | `/auth/logout` | Đăng xuất (thu hồi refresh token) | 204 |

### 8.2. Me endpoint

| Method | Endpoint | Mô tả | Yêu cầu |
|--------|----------|-------|---------|
| GET | `/api/v1/me` | Thông tin user hiện tại | JWT |

### 8.3. System endpoint

| Method | Endpoint | Mô tả | Yêu cầu |
|--------|----------|-------|---------|
| GET | `/api/v1/system/warmup` | Warm-up (UC-18) | Không cần JWT |

### 8.4. Admin Topics (yêu cầu ROLE_ADMIN)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/admin/topics` | Danh sách chủ đề |
| POST | `/admin/topics` | Tạo chủ đề (code snake_case, translations, aliases) |
| GET | `/admin/topics/{id}` | Chi tiết chủ đề |
| PUT | `/admin/topics/{id}` | Cập nhật chủ đề (code cố định) |
| PATCH | `/admin/topics/{id}` | Vô hiệu hóa/kích hoạt |
| DELETE | `/admin/topics/{id}` | Xoá (409 nếu có liên kết) |
| GET | `/admin/topics/{id}/aliases` | Danh sách alias |
| POST | `/admin/topics/{id}/aliases` | Thêm alias |
| DELETE | `/admin/topics/{id}/aliases/{aliasId}` | Xoá alias |

### 8.5. Admin Questions (yêu cầu ROLE_ADMIN)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/admin/questions` | Danh sách câu hỏi |
| POST | `/admin/questions` | Tạo câu hỏi DRAFT |
| GET | `/admin/questions/{id}` | Chi tiết câu hỏi + key points |
| PUT | `/admin/questions/{id}` | Cập nhật |
| DELETE | `/admin/questions/{id}` | Xoá |
| POST | `/admin/questions/{id}/publish` | Publish + trigger indexing (422 nếu thiếu field) |
| POST | `/admin/questions/{id}/archive` | Archive (gỡ khỏi RAG) |

### 8.6. Admin Documents (yêu cầu ROLE_ADMIN)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/admin/documents` | Danh sách tài liệu |
| POST | `/admin/documents` | Tạo + auto-indexing |
| GET | `/admin/documents/{id}` | Chi tiết (kèm nội dung) |
| PUT | `/admin/documents/{id}` | Cập nhật (không tự reindex) |
| DELETE | `/admin/documents/{id}` | Xoá |
| POST | `/admin/documents/{id}/reindex` | Reindex (202 Accepted) |

### 8.7. Admin Users (yêu cầu ROLE_ADMIN)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/admin/users?role=&status=` | Danh sách (lọc tùy chọn) |
| GET | `/admin/users/{id}` | Chi tiết user |
| PATCH | `/admin/users/{id}/status` | Đổi status (ACTIVE/LOCKED) |

### 8.8. AI Service (yêu cầu `X-Internal-Key` header)

Tất cả ở `/ai/v1/**` — chỉ Java backend gọi, không expose ra frontend.

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/ai/v1/health` | Health check (không cần key) |
| POST | `/ai/v1/index/questions` | Index câu hỏi vào RAG |
| DELETE | `/ai/v1/index/questions/{id}` | Xoá câu hỏi khỏi RAG |
| POST | `/ai/v1/index/documents` | Index tài liệu (chunk + embedding) |
| DELETE | `/ai/v1/index/documents/{id}` | Xoá tài liệu khỏi RAG |

### 8.9. Ví dụ gọi API với JWT

```bash
# Đăng nhập → lấy token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"ChangeMe123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Gọi API Admin với JWT
curl http://localhost:8080/api/v1/admin/topics \
  -H "Authorization: Bearer $TOKEN"

# Tạo chủ đề mới
curl -X POST http://localhost:8080/api/v1/admin/topics \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "design_patterns",
    "parentId": null,
    "translations": [
      {"language": "vi", "name": "Design Patterns", "description": "Các mẫu thiết kế phổ biến"},
      {"language": "en", "name": "Design Patterns", "description": "Common design patterns"}
    ],
    "aliases": ["Pattern", "Mẫu thiết kế", "Singleton", "Factory"]
  }'
```

### 8.10. Ví dụ gọi AI Service (internal)

```bash
# Health check (không cần key)
curl http://localhost:8000/ai/v1/health

# Index câu hỏi (cần X-Internal-Key)
curl -X POST http://localhost:8000/ai/v1/index/questions \
  -H "X-Internal-Key: dev-internal-key" \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": "11111111-1111-1111-1111-111111111111",
    "topicCode": "oop",
    "language": "vi",
    "level": "MID",
    "content": "Giải thích tính đóng gói (encapsulation) trong OOP",
    "referenceAnswer": "Encapsulation là việc ẩn chi tiết triển khai...",
    "keyPoints": [
      {"seq": 1, "content": "Ẩn dữ liệu với private/protected"},
      {"seq": 2, "content": "Truy cập qua getter/setter"}
    ]
  }'
```

---

## 9. Chạy kiểm thử

### 9.1. Java Backend

```bash
cd backend
mvn test
```

Test chạy trên H2 in-memory database (không cần Neon):
- `InterviewApplicationTests` — context load
- Integration tests cho Auth, Topics, Questions, Documents, Users

### 9.2. Python AI Service

```bash
cd ai-service
source .venv/bin/activate
pytest tests/ -v
```

Bao gồm:
- `test_health.py` — 5 test health endpoint
- `test_mock_provider.py` — 8 test mock embedding provider

### 9.3. Frontend

```bash
cd frontend
npm test
```

Bao gồm:
- `auth.test.tsx` — login/logout, token storage
- `adminCrud.test.tsx` — Admin CRUD Topics/Questions/Documents/Users

---

## 10. Khắc phục sự cố thường gặp

### Backend không khởi động — lỗi kết nối database

```
Cause: không tìm thấy database hoặc sai credentials
Fix: kiểm tra DATABASE_URL, DB_USERNAME, DB_PASSWORD trong .env
     đảm bảo Neon branch đang active (Neon auto-suspend sau idle)
```

application.yml có `initialization-fail-timeout: -1` để retry khi Neon compute vừa thức dậy.

### Flyway migration lỗi — pgcrypto extension

```
Cause: extension pgcrypto chưa được bật
Fix: Neon dashboard → Settings → Extensions → bật pgcrypto và pgvector
```

### Flyway V010 lỗi — admin seed

```
Cause: placeholder admin_email/admin_password không được thay thế
Fix: đảm bảo ADMIN_EMAIL và ADMIN_PASSWORD có trong môi trường trước khi chạy mvn flyway:migrate
     Hoặc dùng giá trị mặc định (admin@example.com / ChangeMe123!)
```

### AI Service lỗi import torch

```
Cause: torch/sentence-transformers vô tình được cài
Fix: kiểm tra requirements.txt — KHÔNG có torch. Tạo lại venv sạch:
     rm -rf .venv && python -m venv .venv && source .venv/bin/activate
     pip install -r requirements.txt
```

### Frontend 401 trên mọi API call

```
Cause: JWT hết hạn hoặc chưa đăng nhập
Fix: đăng nhập lại. Access token TTL mặc định 60 phút (JWT_ACCESS_TTL_MINUTES)
     Refresh token TTL 7 ngày (JWT_REFRESH_TTL_DAYS)
```

### CORS error

```
Cause: allowed origins không khớp
Fix: đảm bảo ALLOWED_ORIGINS trong .env chứa http://localhost:5173
```

### Admin không đăng nhập được

```
Cause: migration V010 chưa chạy hoặc sai credentials
Fix: 
  1. Kiểm tra migration đã chạy: mvn flyway:info
  2. Nếu chưa: mvn flyway:migrate
  3. Dùng đúng email/password từ ADMIN_EMAIL/ADMIN_PASSWORD
  4. Mặc định: admin@example.com / ChangeMe123!
```

### Candidate bị chuyển về /login khi vào /admin

Đây là hành vi đúng (AC-RBAC-01). Route guard kiểm tra `role === 'ADMIN'`; Candidate → redirect `/login`.

---

## 11. Triển khai production

| Dịch vụ | Nền tảng | Ghi chú |
|---------|----------|---------|
| Frontend | Vercel | Static hosting (`npm run build` → `dist/`) |
| Java Backend | Render | Free web service (512 MB) |
| Python AI Service | Koyeb | Free web service (512 MB) |
| Database | Neon | Main branch (production) |

**Biến môi trường production:**

- Tạo `JWT_SECRET` mới (≥32 byte): `openssl rand -base64 48`
- Tạo `INTERNAL_API_KEY` mới
- Đổi `ADMIN_EMAIL` + `ADMIN_PASSWORD` trước khi migration
- Đặt `EMBEDDING_PROVIDER=gemini` (hoặc `groq`) + cung cấp API key
- Cập nhật `ALLOWED_ORIGINS` thành domain production
- Cập nhật `AI_SERVICE_BASE_URL` thành URL Koyeb

> Xem chi tiết: `docs/architecture/overview.md` và `docs/decisions/open-questions.md`
