# Spec 01 — Foundation & Content Management

> **Trạng thái:** Chờ phê duyệt (không implement code trước khi chủ dự án xác nhận)
> **Nguồn gốc:** `docs/source/SRS-extracted.md` (authoritative), `.kiro/steering/`
> **Phạm vi:** User Registration & Authentication · JWT & RBAC · Admin Topic Management · Question Bank Management · Study Material Management · RAG Indexing Integration
> **Ngôn ngữ tài liệu:** Tiếng Việt (theo NFR-26)
> **Cập nhật:** 2026-10-16

---

## 1. Mục tiêu

Spec này xây dựng **Tầng 1 — Lát cắt 1** theo thứ tự ưu tiên SRS §9.6: nền tảng xác thực người dùng và bộ máy quản lý nội dung cho Admin. Khi hoàn thành, Admin có thể tự nạp toàn bộ dữ liệu (chủ đề, câu hỏi, tài liệu) qua giao diện web mà không cần chạy script; dữ liệu đó được lập chỉ mục embedding và sẵn sàng cho phiên phỏng vấn RAG.

Các feature của Lát cắt 2–4 (JD analysis, interview, reporting) phụ thuộc vào spec này và không thể bắt đầu trước khi spec này hoàn thành.

---

## 2. Actors

| Actor | Vai trò trong spec này |
|---|---|
| Candidate | Đăng ký, đăng nhập, đăng xuất, xem hồ sơ (Should) |
| Admin | Đăng nhập; CRUD chủ đề, câu hỏi, tài liệu; quản lý người dùng; kích hoạt lập chỉ mục |
| Python AI Service | Nhận yêu cầu lập chỉ mục từ Java; tạo embedding; lưu vào schema `rag` |

---

## 3. Yêu cầu chức năng

### 3.1 M1 — Identity & Access (Xác thực và phân quyền)

#### FR-01 · Must · 💡
**Đăng ký, đăng nhập, đăng xuất bằng email và mật khẩu.**

Tiêu chí chi tiết:
- Đăng ký yêu cầu email hợp lệ (format RFC 5322) và mật khẩu tối thiểu 8 ký tự.
- Email phải là duy nhất trong hệ thống; trùng email → HTTP 409 với thông báo rõ ràng.
- Đăng nhập thành công trả về `access_token` (JWT) và `refresh_token`.
- Đăng xuất phía client: client xoá token; phía server: không cần blacklist ở MVP (⚠️ quyết định cần xác nhận — xem open-questions.md B-11 và D-03).
- MVP **không yêu cầu xác minh email** (⚠️ quyết định cần xác nhận — xem open-questions.md B-02 và D-02).

Liên quan: UC-01, US (baseline), AC (baseline), NFR-17, NFR-19.

#### FR-01-JWT · Must · Dẫn xuất từ NFR-17
**JWT access token và refresh token.**

Tiêu chí chi tiết:
- `access_token`: JWT HS256 hoặc RS256, TTL cấu hình được (mặc định đề xuất 60 phút).
- `refresh_token`: opaque token, lưu server-side (DB hoặc Redis-lite), TTL 7 ngày, single-use.
- Endpoint `POST /auth/refresh` cấp access token mới khi refresh token còn hạn.
- Khi refresh token hết hạn hoặc không hợp lệ → HTTP 401, client phải đăng nhập lại.
- Thời gian sống access token đủ dài để không hết hạn giữa phiên phỏng vấn thông thường (tối đa 90 phút).
- ⚠️ Chi tiết refresh strategy cần được chốt ở D-03 trước khi implement M1.

Liên quan: NFR-17, NFR-16, open-questions.md D-03.

#### FR-RBAC · Must · Dẫn xuất từ NFR-19
**Phân quyền theo vai trò CANDIDATE và ADMIN.**

Tiêu chí chi tiết:
- Mỗi user có đúng một `role`: `CANDIDATE` hoặc `ADMIN`.
- Admin đầu tiên được seed qua script khởi tạo DB (Flyway migration); không có API tự đăng ký Admin.
- Endpoint `/admin/**` yêu cầu role `ADMIN`; Candidate gọi → HTTP 403.
- Endpoint Candidate yêu cầu role `CANDIDATE` hoặc `ADMIN` (Admin có thể xem giao diện Candidate để kiểm tra).
- Candidate A không thể truy cập dữ liệu của Candidate B → HTTP 404 hoặc 403.

Liên quan: NFR-19, BR-15, FR-34, open-questions.md B-09.

#### FR-34 · Must · ✅
**Admin quản lý người dùng: khóa và mở khóa tài khoản.**

Tiêu chí chi tiết:
- Admin xem danh sách users với filter theo role và status.
- Admin thay đổi status: `ACTIVE` ↔ `LOCKED`.
- User bị khóa: đăng nhập trả HTTP 403; refresh token bị thu hồi ngay lập tức.
- Admin không thể tự khóa tài khoản của mình.

Liên quan: UC-16a, US-19, NFR-19.

---

### 3.2 M2 — Content Management (Quản lý nội dung)

#### FR-37 · Must · 💡
**Admin quản lý danh mục chủ đề và từ khóa đồng nghĩa (alias).**

Tiêu chí chi tiết:
- CRUD chủ đề: mỗi chủ đề có `code` (unique, snake_case, bất biến sau khi tạo), `name` (tiếng Việt), `description`, `parent_id` (hỗ trợ cây 1 cấp), `is_active`.
- Mỗi chủ đề có danh sách alias: chuỗi từ khóa dùng để ánh xạ JD (ví dụ "OOP", "Lập trình hướng đối tượng", "Object-Oriented").
- Xóa chủ đề chỉ cho phép khi không có câu hỏi hoặc tài liệu nào liên kết; ngược lại → HTTP 409.
- Vô hiệu hóa chủ đề (`is_active = false`) thay cho xóa khi đã có liên kết.
- Tên và mô tả lưu theo ngôn ngữ qua bảng `topic_translations` (chuẩn bị đa ngôn ngữ, NFR-27).
- Seed dữ liệu ~8 chủ đề mặc định qua Flyway migration (không phải script tay).

Liên quan: UC-14, US-15, BR-07, NFR-27, §9.4.

#### FR-32 · Must · ✅
**Admin CRUD ngân hàng câu hỏi kèm đáp án chuẩn và ý chính.**

Tiêu chí chi tiết:
- Câu hỏi có: `topic_id`, `language` (mặc định `vi`), `level` (`JUNIOR`/`MID`/`SENIOR`), `content`, `reference_answer`, `status` (`DRAFT`/`PUBLISHED`).
- Mỗi câu hỏi có 1–N `key_points` (danh sách ý chính, có thứ tự `seq`).
- **Xuất bản (publish):** Admin gọi `POST /admin/questions/{id}/publish`; hệ thống validate đủ trường bắt buộc trước khi xuất bản (BR-19): topic, level, content, reference_answer, ít nhất 1 key_point.
- Câu hỏi thiếu trường bắt buộc khi publish → HTTP 422 với danh sách trường còn thiếu (AC-18).
- Câu hỏi `DRAFT` không được chọn vào pool RAG (AC-19).
- Xóa câu hỏi `PUBLISHED` không được phép nếu câu hỏi đã được dùng trong ít nhất một phiên; phải archive thay thế.

Liên quan: UC-14, US-14, AC-18, AC-19, BR-02, BR-19.

#### FR-39 · Should · 💡
**Admin nhập hàng loạt câu hỏi từ CSV hoặc JSON.**

Tiêu chí chi tiết:
- Hỗ trợ file `.csv` và `.json` (tối đa 200 câu mỗi lần upload).
- Validate từng dòng; trả báo cáo lỗi chi tiết (dòng N, lỗi gì) mà không rollback toàn bộ.
- Câu hỏi import vào trạng thái `DRAFT`; cần publish riêng từng câu hoặc batch.
- Format CSV/JSON được document trong API docs.

Liên quan: UC-14, BR-19.

---

### 3.3 M2 + M3 — Study Material & RAG Indexing

#### FR-33 · Must · ✅
**Admin quản lý tài liệu kiến thức và lập chỉ mục embedding cho RAG.**

Tiêu chí chi tiết:
- Admin CRUD tài liệu: mỗi tài liệu có `topic_id`, `language`, `title`, `content_md` (Markdown).
- Lập chỉ mục (indexing) được kích hoạt **thủ công** qua `POST /admin/documents/{id}/reindex` hoặc **tự động** khi lưu tài liệu lần đầu.
- Java gọi Python AI Service (`POST /ai/v1/index/documents`) với payload tài liệu; Python chunk, embed và lưu vào schema `rag`.
- Khi indexing thành công, `status` tài liệu chuyển thành `INDEXED`; khi thất bại → `INDEX_FAILED` với `error_message` lưu lại.
- Tài liệu chưa `INDEXED` không được hiện ra trong study materials (AC-19 tương tự cho documents).
- Admin có thể kích hoạt reindex bất kỳ lúc nào (sau khi sửa nội dung).

Liên quan: UC-15, AC-19, NFR-15, FR-28.

#### FR-33-questions-index · Must · Dẫn xuất từ FR-33 + FR-15
**Admin xuất bản câu hỏi kích hoạt lập chỉ mục vector cho RAG.**

Tiêu chí chi tiết:
- Khi Admin publish câu hỏi, Java gọi `POST /ai/v1/index/questions` với payload câu hỏi.
- Python tạo embedding cho `content` + `reference_answer` và lưu vào `rag_question_vectors`.
- Nếu indexing thất bại, câu hỏi vẫn được publish nhưng có cờ `index_status = INDEX_FAILED`; Admin thấy cảnh báo và có thể reindex thủ công.
- Câu hỏi `INDEX_FAILED` không được chọn vào pool RAG.

Liên quan: FR-15, FR-32, AC-19.

---

## 4. Yêu cầu phi chức năng áp dụng trong spec này

| ID | Nội dung | Ảnh hưởng |
|---|---|---|
| NFR-06 | Chỉ dùng embedding API miễn phí; không load torch/sentence-transformers | Python AI Service |
| NFR-12 | Mỗi service ≤512 MB RAM | Java heap config, Python không load model nặng |
| NFR-17 | Mật khẩu băm bcrypt hoặc argon2; xác thực JWT | M1 |
| NFR-19 | Phân quyền CANDIDATE / ADMIN | M1, M2 |
| NFR-24 | Ghi log mỗi lần gọi AI (purpose, provider, model, latency, error) | M3 / M8 |
| NFR-27 | Không hard-code chuỗi; ngôn ngữ là thuộc tính của nội dung | M2 |
| NFR-29 | Ngưỡng và cấu hình không hard-code; lưu vào `scoring_settings` | M1/M2 bootstrap |
| NFR-30 | JVM heap tinh chỉnh; Python không dùng thư viện ML nặng | Cả hai service |
| NFR-33 | Phát triển trên máy 4GB không GPU; dùng nhánh Neon dev; mock AI provider trong test | M3 |

---

## 5. Quy tắc nghiệp vụ áp dụng

| ID | Nội dung |
|---|---|
| BR-02 | Mỗi câu hỏi phải có đáp án chuẩn và danh sách ý chính để chấm có cơ sở |
| BR-15 | Chỉ người sở hữu mới xem được dữ liệu của mình (áp dụng cho Candidate) |
| BR-19 | Admin chỉ publish câu hỏi khi đủ trường: chủ đề, level, nội dung, đáp án chuẩn, ≥1 ý chính |

---

## 6. Acceptance Criteria

| ID | Given → When → Then | SRS ref |
|---|---|---|
| AC-AUTH-01 | Given email chưa tồn tại, password hợp lệ / When POST /auth/register / Then HTTP 201, user ACTIVE, password không xuất hiện trong response | FR-01, NFR-17 |
| AC-AUTH-02 | Given email đã tồn tại / When POST /auth/register / Then HTTP 409, message rõ ràng | FR-01 |
| AC-AUTH-03 | Given credentials hợp lệ / When POST /auth/login / Then HTTP 200, trả `access_token` (JWT) và `refresh_token` | FR-01, NFR-17 |
| AC-AUTH-04 | Given credentials sai / When POST /auth/login / Then HTTP 401 | FR-01 |
| AC-AUTH-05 | Given refresh_token hợp lệ còn hạn / When POST /auth/refresh / Then HTTP 200, access_token mới | FR-01-JWT |
| AC-AUTH-06 | Given refresh_token hết hạn hoặc không hợp lệ / When POST /auth/refresh / Then HTTP 401 | FR-01-JWT |
| AC-AUTH-07 | Given user bị LOCKED / When POST /auth/login / Then HTTP 403 | FR-34, NFR-19 |
| AC-RBAC-01 | Given Candidate JWT / When GET /admin/topics / Then HTTP 403 | FR-RBAC, NFR-19 |
| AC-RBAC-02 | Given không có JWT / When GET /api/v1/topics / Then HTTP 401 | FR-RBAC |
| AC-TOPIC-01 | Given Admin JWT, body hợp lệ / When POST /admin/topics / Then HTTP 201, topic tồn tại trong DB | FR-37 |
| AC-TOPIC-02 | Given topic có câu hỏi liên kết / When DELETE /admin/topics/{id} / Then HTTP 409 | FR-37 |
| AC-TOPIC-03 | Given topic không có câu hỏi / When PATCH /admin/topics/{id} với is_active=false / Then HTTP 200, topic inactive | FR-37 |
| AC-Q-01 (≡ AC-18) | Given Admin lưu câu hỏi thiếu reference_answer / When POST /admin/questions/{id}/publish / Then HTTP 422, danh sách trường thiếu | FR-32, BR-19, AC-18 |
| AC-Q-02 (≡ AC-19) | Given câu hỏi DRAFT / When truy xuất RAG / Then câu hỏi không xuất hiện trong pool | FR-32, AC-19 |
| AC-Q-03 | Given câu hỏi đủ trường / When POST /admin/questions/{id}/publish / Then HTTP 200, status PUBLISHED, indexing triggered | FR-32, FR-33-questions-index |
| AC-Q-04 | Given Admin import CSV hợp lệ / When POST /admin/questions/import / Then HTTP 200, báo cáo số câu thành công/lỗi | FR-39 |
| AC-DOC-01 | Given tài liệu mới lưu / When indexing thành công / Then status INDEXED; nội dung có thể truy xuất / given chưa index / Then không xuất hiện trong study materials | FR-33, AC-19 |
| AC-DOC-02 | Given Python AI Service không khả dụng khi Admin lưu tài liệu / When save / Then document được lưu nhưng status = INDEX_FAILED, Java không throw lỗi cho Admin | FR-33, NFR-10 |
| AC-USER-01 | Given Admin / When PATCH /admin/users/{id}/status với LOCKED / Then user không thể đăng nhập, refresh token bị thu hồi | FR-34 |
| AC-USER-02 | Given Admin / When cố tự khóa tài khoản mình / Then HTTP 400 | FR-34 |
| AC-LOG-01 | Given bất kỳ lời gọi /ai/v1/index/* / When gọi xong / Then bản ghi ai_call_logs được tạo với purpose, provider, latency_ms, status | NFR-24 |

---

## 7. Dữ liệu tối thiểu (seed) cần có sau khi hoàn thành spec

Theo SRS §9.4, các dữ liệu sau phải được seed qua Flyway migration (không script tay):
- ~8 chủ đề mặc định: OOP, Core Java, Collections, Java 8+ (Lambda/Stream), Concurrency, JVM & Memory, Spring Boot, Database & JPA.
- Mỗi chủ đề có alias tiếng Việt và tiếng Anh.
- 1 tài khoản Admin mặc định (email + password từ biến môi trường).
- Bảng `scoring_settings` với các giá trị mặc định: follow-up threshold 70%, max follow-ups 2, rubric weights 50/30/10/10, weak threshold 50%.

---

## 8. Ngoài phạm vi spec này

- Phân tích JD, tạo phiên phỏng vấn (Spec 02).
- Interview Orchestrator, chấm điểm, follow-up (Spec 03).
- Báo cáo, kiến thức yếu (Spec 04).
- Voice interview (Won't - FR-36).
- CV upload (Removed - FR-03, 05, 08).
- Biểu đồ tiến bộ, xuất PDF (Could - FR-30, 41).
- Tải file JD (Should - FR-42, thuộc Spec 02).

---

## 9. Các quyết định cần xác nhận trước khi bắt đầu implement

> Xem chi tiết ở `docs/decisions/open-questions.md`

| # | Quyết định | ID | Deadline |
|---|---|---|---|
| D-01 | Frontend: React/Vite (đề xuất) hay Vue? | Toàn bộ FE | Trước Sprint 1 |
| D-02 | MVP có xác minh email khi đăng ký không? | FR-01, B-02 | Trước implement M1 |
| D-03 | JWT refresh strategy: stateless access-only hay có refresh endpoint? | NFR-17, B-03 | Trước implement M1 |

---

*Tài liệu này chờ phê duyệt của chủ dự án. Không implement code cho đến khi nhận được phê duyệt rõ ràng.*
