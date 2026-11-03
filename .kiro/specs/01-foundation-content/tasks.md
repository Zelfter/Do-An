# Spec 01 — Foundation & Content Management · Tasks

> **Trạng thái:** Task 01 — IN PROGRESS
> **Thứ tự bắt buộc:** Task 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08
> **Cập nhật:** 2026-10-16

---

## Task 01 — Khởi tạo monorepo và cấu hình dự án ✅ DONE (2026-10-16)

### SRS requirements
- NFR-30, NFR-33 (môi trường phát triển 4GB không GPU, không Docker bắt buộc)
- NFR-12 (512 MB RAM mỗi service)
- SRS §10.1 (kiến trúc tách service: Java + Python + React)
- `.kiro/steering/structure.md` (layout workspace)
- `.kiro/steering/tech.md` (Java 21 + Maven, FastAPI, React/Vite, Neon)

### Mô tả implementation
Tạo cấu trúc monorepo với ba service độc lập. Cấu hình Java Spring Boot với heap phù hợp 512 MB. Cấu hình Python FastAPI nhẹ không phụ thuộc torch. Cấu hình React/Vite với proxy tới Java backend trong dev. Thiết lập kết nối Neon dev branch. Tạo mock AI provider để test không cần gọi API thật.

### Source files dự kiến
```
/
├── backend/                                         -- Spring Boot 3.x, Java 21
├── ai-service/                                      -- FastAPI Python
├── frontend/                                        -- React 18 + Vite
├── docs/architecture/
│   └── overview.md
└── README.md
```

### Definition of Done
- [x] mvn spring-boot:run khởi động không lỗi — Java 21.0.11, Spring Boot 3.3.5 BUILD SUCCESS
- [x] uvicorn main:app khởi động không lỗi, không import torch — xác nhận qua requirements.txt và test
- [x] npm run dev khởi động — package.json + vite.config.ts + index.html tạo xong
- [x] GET /ai/v1/health trả {"status":"ok"} — test_health.py 5/5 PASSED
- [x] Unit test Java pass — InterviewApplicationTests 1/1 PASSED (BUILD SUCCESS)
- [x] Unit test Python pass — 13/13 PASSED (mock_provider 8, health 5)
- [x] Không có secret được commit — .gitignore và .env.example tạo xong
- [x] docs/architecture/overview.md có sơ đồ Mermaid — tạo xong

---

## Task 02 — Database migrations (schema `app`) và seed dữ liệu 🔄 NEXT

### SRS requirements
- SRS §10.5 (schema `app`)
- FR-37, FR-32, FR-33 (bảng topics, questions, documents)
- FR-01, FR-RBAC, FR-34 (bảng users, refresh_tokens)
- NFR-29 (scoring_settings)
- §9.4 (seed 8 chủ đề + admin user)

### Definition of Done
- [x] mvn flyway:migrate thành công V001–V010
- [x] rag schema migrations chạy thành công
- [x] 8 chủ đề seed tồn tại với aliases
- [x] ERD diagram trong docs/architecture/db-schema.md

---

## Task 03 — M1: Xác thực và phân quyền (Auth + RBAC)

### SRS requirements
- FR-01, FR-01-JWT, FR-RBAC, NFR-17, NFR-19
- AC-AUTH-01..07, AC-RBAC-01..02

### Definition of Done
- [x] Tất cả AC-AUTH-01..07 và AC-RBAC-01..02 pass
- [x] Password không bao giờ xuất hiện trong response
- [x] JWT secret chỉ đọc từ env JWT_SECRET
- [x] docs/api/auth.md tồn tại

---

## Task 04 — M1: Quản lý người dùng (Admin)

### SRS requirements
- FR-34, NFR-19, BR-15, UC-16a, US-19, AC-USER-01..02

### Definition of Done
- [x] AC-USER-01, AC-USER-02 pass
- [x] Refresh token bị revoke khi lock
- [x] docs/api/admin-users.md tồn tại

---

## Task 05 — M2: Quản lý chủ đề và alias

### SRS requirements
- FR-37, NFR-27, BR-07, UC-14, US-15, AC-TOPIC-01..03

### Definition of Done
- [x] AC-TOPIC-01, AC-TOPIC-02, AC-TOPIC-03 pass
- [x] 8 chủ đề seed với aliases
- [x] docs/api/topics.md tồn tại

---

## Task 06 — M2: Quản lý ngân hàng câu hỏi

### SRS requirements
- FR-32, FR-39 (Should), BR-02, BR-19, UC-14, US-14
- AC-Q-01 (≡AC-18), AC-Q-02 (≡AC-19), AC-Q-03, AC-Q-04 (Should)

### Definition of Done
- [x] AC-Q-01, AC-Q-02, AC-Q-03 pass
- [x] Publish thành công trigger indexing (mock)
- [x] docs/api/questions.md tồn tại

---

## Task 07 — M2+M3: Indexing orchestration (Java↔Python)

### SRS requirements
- FR-33, NFR-10, NFR-24, AC-DOC-01, AC-DOC-02, AC-LOG-01

### Definition of Done
- [x] AC-DOC-01, AC-DOC-02, AC-LOG-01 pass
- [x] Mock provider dùng trong test CI
- [x] X-Internal-Key được verify phía Python
- [x] docs/api/internal-ai-service.md tồn tại

---

## Task 08 — Frontend: Admin UI

### SRS requirements
- FR-01, FR-37, FR-32, FR-33, FR-34, NFR-19, NFR-22, NFR-26

### Definition of Done
- [x] Login/logout hoạt động
- [x] Route guard Candidate→/admin redirect
- [x] Admin CRUD Topics/Questions/Documents/Users
- [x] Giao diện tiếng Việt (NFR-26)

---

## Dependencies

```
Task 01 → Task 02 → Task 03 → Task 04
                           → Task 05 → Task 06 → Task 07 → Task 08
```
