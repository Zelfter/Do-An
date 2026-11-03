# API — Quản lý ngân hàng câu hỏi (Admin Questions)

> Task 06 · M2 · FR-32 · FR-33-questions-index · FR-39
> AC-Q-01 (≡ AC-18), AC-Q-02 (≡ AC-19), AC-Q-03, AC-Q-04, AC-LOG-01

## Tổng quan

- Các endpoint quản lý câu hỏi dành cho **Admin** (FR-32): tạo, xem, cập nhật, xóa câu hỏi và thao tác vòng đời (publish, archive, reindex).
- Mỗi câu hỏi thuộc một **chủ đề** (`topicId`), có **cấp độ** (`JUNIOR`/`MID`/`SENIOR`), **ngôn ngữ** (`vi`/`en`…), **đáp án chuẩn** (`referenceAnswer`) và **danh sách ý chính** (`keyPoints`).
- **Trạng thái vòng đời:** `DRAFT` → `PUBLISHED` → `ARCHIVED`. Chỉ Admin mới thay đổi trạng thái.
- **BR-19 — Quy tắc publish:** câu hỏi chỉ được publish khi đủ 5 điều kiện: `topicId`, `level`, `content`, `referenceAnswer`, và ít nhất 1 `keyPoint`. Thiếu → **422** với danh sách trường còn thiếu (AC-Q-01).
- **AC-Q-02:** Chỉ câu hỏi `PUBLISHED` + `INDEXED` mới được chọn vào pool RAG; câu hỏi `DRAFT` không xuất hiện trong pool.
- **AC-Q-03:** Khi publish thành công, Java tự động gọi Python AI Service để lập chỉ mục vector (`POST /ai/v1/index/questions`).
- **FR-39:** Hỗ trợ nhập hàng loạt từ CSV hoặc JSON (tối đa 200 câu/lần); câu hỏi import vào trạng thái `DRAFT`.
- Tất cả route nằm dưới `/api/v1/admin/questions` → yêu cầu `ROLE_ADMIN` (FR-RBAC, NFR-19). Candidate gọi → **403**; thiếu/không hợp lệ JWT → **401**.

## Phân quyền (RBAC)

| Endpoint                                          | Quyền yêu cầu | Không thỏa →                              |
|---------------------------------------------------|--------------|-------------------------------------------|
| `GET /api/v1/admin/questions`                     | `ROLE_ADMIN` | Candidate → 403 (AC-RBAC-01), Không JWT → 401 (AC-RBAC-02) |
| `POST /api/v1/admin/questions`                    | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `GET /api/v1/admin/questions/{id}`                | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `PUT /api/v1/admin/questions/{id}`                | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `DELETE /api/v1/admin/questions/{id}`             | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `POST /api/v1/admin/questions/{id}/publish`       | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `POST /api/v1/admin/questions/{id}/archive`       | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `POST /api/v1/admin/questions/{id}/reindex`       | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |
| `POST /api/v1/admin/questions/import`             | `ROLE_ADMIN` | Candidate → 403, Không JWT → 401         |

RoleHierarchy: `ADMIN > CANDIDATE` — Admin thừa hưởng quyền Candidate (FR-RBAC).

---

## Mô hình dữ liệu (Data models)

### QuestionDetailDto

Shape đầy đủ trả về bởi `GET /questions/{id}` và sau khi `POST /questions`, `PUT /questions/{id}`:

```json
{
  "id": "a1b2...uuid",
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP và cho ví dụ thực tế?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc: Encapsulation, Inheritance, Polymorphism, Abstraction...",
  "status": "DRAFT",
  "indexStatus": "NOT_INDEXED",
  "keyPoints": [
    { "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ qua getter/setter" },
    { "seq": 2, "content": "Inheritance: tái sử dụng code qua kế thừa" },
    { "seq": 3, "content": "Polymorphism: một interface, nhiều cách thực thi" },
    { "seq": 4, "content": "Abstraction: ẩn chi tiết, chỉ lộ giao diện cần thiết" }
  ],
  "createdAt": "2026-10-16T08:00:00Z",
  "updatedAt": "2026-10-16T08:00:00Z"
}
```

| Trường            | Kiểu            | Mô tả                                                                           |
|-------------------|-----------------|---------------------------------------------------------------------------------|
| `id`              | UUID            | Id của câu hỏi.                                                                 |
| `topicId`         | UUID            | Id chủ đề mà câu hỏi thuộc về.                                                  |
| `language`        | string          | Mã ngôn ngữ (VD: `vi`, `en`). Mặc định `vi`.                                   |
| `level`           | enum            | Cấp độ: `JUNIOR`, `MID`, hoặc `SENIOR`.                                        |
| `content`         | string          | Nội dung câu hỏi (tối đa 5000 ký tự).                                          |
| `referenceAnswer` | string          | Đáp án chuẩn (tham khảo). Bắt buộc để publish (BR-19).                         |
| `status`          | enum            | Trạng thái vòng đời: `DRAFT`, `PUBLISHED`, hoặc `ARCHIVED`.                    |
| `indexStatus`     | enum            | Trạng thái lập chỉ mục: `NOT_INDEXED`, `INDEXING`, `INDEXED`, `INDEX_FAILED`.  |
| `keyPoints`       | KeyPointDto[]   | Danh sách ý chính theo thứ tự `seq`. Bắt buộc ≥1 để publish (BR-19).          |
| `createdAt`       | ISO 8601        | Thời điểm tạo.                                                                  |
| `updatedAt`       | ISO 8601        | Thời điểm cập nhật gần nhất.                                                    |

### QuestionSummaryDto

Shape rút gọn dùng trong danh sách `GET /questions` (phân trang):

```json
{
  "id": "a1b2...uuid",
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP...",
  "status": "DRAFT",
  "indexStatus": "NOT_INDEXED",
  "keyPointCount": 4,
  "createdAt": "2026-10-16T08:00:00Z",
  "updatedAt": "2026-10-16T08:00:00Z"
}
```

> `referenceAnswer` và `keyPoints` không trả về trong danh sách để giảm tải payload.

### KeyPointDto

```json
{ "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ" }
```

| Trường    | Kiểu   | Mô tả                                        |
|-----------|--------|----------------------------------------------|
| `seq`     | int    | Thứ tự hiển thị (bắt đầu từ 1, tăng dần).  |
| `content` | string | Nội dung ý chính. Không để trống.            |

### Error response (shape dùng chung)

Mọi lỗi trả về theo cùng shape:

```json
{ "timestamp": "...", "status": <mã HTTP>, "message": "<thông báo tiếng Việt>", "errors": [] }
```

Shape đặc biệt cho lỗi 422 (thiếu trường khi publish):

```json
{
  "status": 422,
  "message": "Thiếu trường bắt buộc",
  "missingFields": ["reference_answer", "key_points"]
}
```

---

## GET /api/v1/admin/questions

FR-32: liệt kê câu hỏi có phân trang và lọc.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Query params:**

| Tham số    | Kiểu    | Bắt buộc | Mô tả                                                          |
|------------|---------|----------|----------------------------------------------------------------|
| `page`     | int     | Không    | Số trang (bắt đầu từ 0). Mặc định `0`.                        |
| `size`     | int     | Không    | Số phần tử mỗi trang. Mặc định `20`, tối đa `100`.            |
| `topicId`  | UUID    | Không    | Lọc theo chủ đề.                                               |
| `level`    | enum    | Không    | Lọc theo cấp độ: `JUNIOR`, `MID`, `SENIOR`.                   |
| `status`   | enum    | Không    | Lọc theo trạng thái: `DRAFT`, `PUBLISHED`, `ARCHIVED`.        |
| `language` | string  | Không    | Lọc theo ngôn ngữ (VD: `vi`, `en`).                           |

**Response 200:**
```json
{
  "content": [
    {
      "id": "a1b2...uuid",
      "topicId": "9f1e...uuid",
      "language": "vi",
      "level": "JUNIOR",
      "content": "Giải thích nguyên tắc OOP...",
      "status": "PUBLISHED",
      "indexStatus": "INDEXED",
      "keyPointCount": 4,
      "createdAt": "2026-10-16T08:00:00Z",
      "updatedAt": "2026-10-16T09:00:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

**Response 401** (thiếu hoặc JWT không hợp lệ):
```json
{ "timestamp": "...", "status": 401, "message": "...", "errors": [] }
```

**Response 403** (Candidate JWT):
```json
{ "timestamp": "...", "status": 403, "message": "...", "errors": [] }
```

---

## POST /api/v1/admin/questions

FR-32: tạo câu hỏi mới. Câu hỏi mới luôn ở trạng thái `DRAFT`.

**Header:**
```
Authorization: Bearer <accessToken>
```
> `accessToken` phải có `role=ADMIN`.

**Request:**
```json
{
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP và cho ví dụ thực tế?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc: Encapsulation, Inheritance, Polymorphism, Abstraction...",
  "keyPoints": [
    { "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ qua getter/setter" },
    { "seq": 2, "content": "Inheritance: tái sử dụng code qua kế thừa" }
  ]
}
```

| Trường            | Kiểu          | Bắt buộc | Mô tả                                                                  |
|-------------------|---------------|----------|------------------------------------------------------------------------|
| `topicId`         | UUID          | Có       | Id chủ đề. Phải tồn tại trong hệ thống.                                |
| `language`        | string        | Không    | Mã ngôn ngữ. Mặc định `vi` (NFR-27).                                  |
| `level`           | enum          | Có       | Cấp độ: `JUNIOR`, `MID`, hoặc `SENIOR`.                               |
| `content`         | string        | Có       | Nội dung câu hỏi. Tối đa 5000 ký tự.                                  |
| `referenceAnswer` | string        | Không    | Đáp án chuẩn. Không bắt buộc khi tạo nhưng bắt buộc trước khi publish. |
| `keyPoints`       | KeyPointDto[] | Không    | Danh sách ý chính có thứ tự. Không bắt buộc khi tạo, bắt buộc ≥1 để publish. |

**Response 201:**
```json
{
  "id": "a1b2...uuid",
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP và cho ví dụ thực tế?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc...",
  "status": "DRAFT",
  "indexStatus": "NOT_INDEXED",
  "keyPoints": [
    { "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ qua getter/setter" },
    { "seq": 2, "content": "Inheritance: tái sử dụng code qua kế thừa" }
  ],
  "createdAt": "2026-10-16T08:00:00Z",
  "updatedAt": "2026-10-16T08:00:00Z"
}
```

**Response 400** (thiếu trường bắt buộc hoặc dữ liệu không hợp lệ):
```json
{ "timestamp": "...", "status": 400, "message": "Trường 'level' không được để trống", "errors": [] }
```

**Response 404** (`topicId` không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

---

## GET /api/v1/admin/questions/{id}

FR-32: lấy thông tin đầy đủ một câu hỏi theo id, bao gồm `referenceAnswer` và `keyPoints`.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                    |
|---------|------|--------------------------|
| `id`    | UUID | Id của câu hỏi cần xem.  |

**Response 200:**
```json
{
  "id": "a1b2...uuid",
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP và cho ví dụ thực tế?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc: Encapsulation, Inheritance, Polymorphism, Abstraction...",
  "status": "DRAFT",
  "indexStatus": "NOT_INDEXED",
  "keyPoints": [
    { "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ qua getter/setter" },
    { "seq": 2, "content": "Inheritance: tái sử dụng code qua kế thừa" },
    { "seq": 3, "content": "Polymorphism: một interface, nhiều cách thực thi" },
    { "seq": 4, "content": "Abstraction: ẩn chi tiết, chỉ lộ giao diện cần thiết" }
  ],
  "createdAt": "2026-10-16T08:00:00Z",
  "updatedAt": "2026-10-16T08:00:00Z"
}
```

**Response 404** (câu hỏi không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy câu hỏi", "errors": [] }
```

---

## PUT /api/v1/admin/questions/{id}

FR-32: cập nhật toàn phần câu hỏi. Chỉ câu hỏi ở trạng thái `DRAFT` hoặc `PUBLISHED` mới được cập nhật (câu hỏi `ARCHIVED` cần unarchive trước).

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                         |
|---------|------|-------------------------------|
| `id`    | UUID | Id của câu hỏi cần cập nhật.  |

**Request:**
```json
{
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "MID",
  "content": "Giải thích nguyên tắc OOP, cho ví dụ thực tế và so sánh với lập trình thủ tục?",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc: Encapsulation, Inheritance, Polymorphism, Abstraction...",
  "keyPoints": [
    { "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ qua getter/setter" },
    { "seq": 2, "content": "Inheritance: tái sử dụng code qua kế thừa" },
    { "seq": 3, "content": "Polymorphism: một interface, nhiều cách thực thi" },
    { "seq": 4, "content": "Abstraction: ẩn chi tiết, chỉ lộ giao diện cần thiết" },
    { "seq": 5, "content": "So sánh OOP vs procedural: OOP tổ chức code theo đối tượng" }
  ]
}
```

| Trường            | Kiểu          | Bắt buộc | Mô tả                                              |
|-------------------|---------------|----------|----------------------------------------------------|
| `topicId`         | UUID          | Có       | Id chủ đề. Phải tồn tại trong hệ thống.            |
| `language`        | string        | Không    | Mã ngôn ngữ. Mặc định `vi`.                        |
| `level`           | enum          | Có       | Cấp độ: `JUNIOR`, `MID`, hoặc `SENIOR`.            |
| `content`         | string        | Có       | Nội dung câu hỏi. Tối đa 5000 ký tự.               |
| `referenceAnswer` | string        | Không    | Đáp án chuẩn.                                      |
| `keyPoints`       | KeyPointDto[] | Không    | Thay thế toàn bộ danh sách ý chính (clear + thêm). |

**Response 200:** trả về `QuestionDetailDto` đã cập nhật (tương tự GET `{id}`).

**Response 400** (dữ liệu không hợp lệ):
```json
{ "timestamp": "...", "status": 400, "message": "Trường 'level' không được để trống", "errors": [] }
```

**Response 404** (câu hỏi hoặc chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy câu hỏi", "errors": [] }
```

---

## DELETE /api/v1/admin/questions/{id}

FR-32: xóa câu hỏi. **Bị chặn (409)** nếu câu hỏi `PUBLISHED` đã được dùng trong ít nhất một phiên phỏng vấn — phải archive thay thế.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                    |
|---------|------|--------------------------|
| `id`    | UUID | Id của câu hỏi cần xóa.  |

**Response 204:**
> Không có body. Trả về khi xóa thành công.

**Response 404** (câu hỏi không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy câu hỏi", "errors": [] }
```

**Response 409** (câu hỏi đã được dùng trong phiên):
```json
{ "timestamp": "...", "status": 409, "message": "Không thể xóa câu hỏi đã được sử dụng trong phiên phỏng vấn. Hãy archive thay thế.", "errors": [] }
```

### Hành vi khi xóa bị chặn

Khi câu hỏi đã xuất hiện trong phiên phỏng vấn, dùng archive thay thế:

```http
POST /api/v1/admin/questions/{id}/archive
Authorization: Bearer <accessToken>
```

---

## POST /api/v1/admin/questions/{id}/publish

AC-Q-01, AC-Q-03, FR-32, BR-19: xuất bản câu hỏi. Hệ thống validate đủ trường theo **BR-19** trước khi publish và tự động kích hoạt lập chỉ mục vector.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                         |
|---------|------|-------------------------------|
| `id`    | UUID | Id của câu hỏi cần xuất bản.  |

**Không có request body.**

**Luồng xử lý:**

```
Admin → POST /publish
  Java: validate đủ trường (BR-19)
    ├─ thiếu → 422 {missingFields: [...]}
    └─ đủ → update status = PUBLISHED
            → gọi POST /ai/v1/index/questions (sync, timeout 40s)
                ├─ 200 → update indexStatus = INDEXED → 200 OK cho Admin
                └─ lỗi → update indexStatus = INDEX_FAILED
                        → log AiCallLog(FAILED)
                        → vẫn trả 200 cho Admin với indexStatus = INDEX_FAILED
```

**Response 200 (publish thành công, đã indexed):**
```json
{
  "id": "a1b2...uuid",
  "topicId": "9f1e...uuid",
  "language": "vi",
  "level": "JUNIOR",
  "content": "Giải thích nguyên tắc OOP...",
  "referenceAnswer": "OOP bao gồm 4 nguyên tắc...",
  "status": "PUBLISHED",
  "indexStatus": "INDEXED",
  "keyPoints": [
    { "seq": 1, "content": "Encapsulation: che giấu trạng thái nội bộ" }
  ],
  "createdAt": "2026-10-16T08:00:00Z",
  "updatedAt": "2026-10-16T09:00:00Z"
}
```

**Response 200 (publish thành công, indexing thất bại — AC-Q-03 + FR-33-questions-index):**
```json
{
  "id": "a1b2...uuid",
  "status": "PUBLISHED",
  "indexStatus": "INDEX_FAILED",
  "createdAt": "2026-10-16T08:00:00Z",
  "updatedAt": "2026-10-16T09:00:00Z"
}
```
> ⚠️ Câu hỏi đã publish nhưng chưa được lập chỉ mục. Dùng `POST /{id}/reindex` để thử lại.

**Response 422 (thiếu trường bắt buộc — AC-Q-01 ≡ AC-18, BR-19):**
```json
{
  "status": 422,
  "message": "Thiếu trường bắt buộc",
  "missingFields": ["reference_answer", "key_points"]
}
```

Các trường có thể thiếu (`missingFields`):

| Giá trị               | Ý nghĩa                                    |
|-----------------------|--------------------------------------------|
| `topic_id`            | Câu hỏi chưa được gán chủ đề.             |
| `level`               | Câu hỏi chưa có cấp độ.                   |
| `content`             | Nội dung câu hỏi trống.                    |
| `reference_answer`    | Đáp án chuẩn chưa được điền.              |
| `key_points`          | Chưa có ý chính nào (cần ≥ 1).           |

**Response 404** (câu hỏi không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy câu hỏi", "errors": [] }
```

---

## POST /api/v1/admin/questions/{id}/archive

FR-32: archive câu hỏi. Câu hỏi `ARCHIVED` không xuất hiện trong pool RAG và không thể chọn vào phiên mới.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                       |
|---------|------|-----------------------------|
| `id`    | UUID | Id của câu hỏi cần archive. |

**Không có request body.**

**Response 200:**
```json
{
  "id": "a1b2...uuid",
  "status": "ARCHIVED",
  "indexStatus": "NOT_INDEXED",
  "updatedAt": "2026-10-16T10:00:00Z"
}
```

**Response 404** (câu hỏi không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy câu hỏi", "errors": [] }
```

---

## POST /api/v1/admin/questions/{id}/reindex

FR-33-questions-index: kích hoạt lập chỉ mục vector thủ công. Dùng khi `indexStatus = INDEX_FAILED` hoặc sau khi sửa nội dung câu hỏi đã publish.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                         |
|---------|------|-------------------------------|
| `id`    | UUID | Id của câu hỏi cần reindex.   |

**Không có request body.**

**Response 202:**
```json
{ "indexJobId": "job-uuid-or-correlation-id" }
```

> Indexing được thực hiện đồng bộ (sync, timeout 40s). `indexJobId` là id tương quan để trace log. Kết quả cuối cùng (`INDEXED` hoặc `INDEX_FAILED`) được cập nhật vào `indexStatus` của câu hỏi.

**Response 404** (câu hỏi không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy câu hỏi", "errors": [] }
```

**Response 409** (câu hỏi chưa được publish — chỉ câu hỏi PUBLISHED mới có thể reindex):
```json
{ "timestamp": "...", "status": 409, "message": "Chỉ câu hỏi ở trạng thái PUBLISHED mới có thể reindex", "errors": [] }
```

---

## POST /api/v1/admin/questions/import

FR-39, AC-Q-04: nhập hàng loạt câu hỏi từ file CSV hoặc JSON. Tối đa 200 câu mỗi lần upload.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
Content-Type: multipart/form-data
```

**Form data:**

| Tham số | Kiểu | Bắt buộc | Mô tả                                     |
|---------|------|----------|-------------------------------------------|
| `file`  | file | Có       | File CSV (`.csv`) hoặc JSON (`.json`).    |

### Định dạng CSV

```csv
topicCode,language,level,content,referenceAnswer,keyPoints
oop,vi,JUNIOR,"Giải thích OOP?","OOP bao gồm 4 nguyên tắc...","Encapsulation|Inheritance|Polymorphism|Abstraction"
core_java,vi,MID,"HashMap hoạt động như thế nào?","HashMap sử dụng hash table...","Hashing|Collision handling|Load factor"
```

| Cột               | Kiểu   | Bắt buộc | Mô tả                                                       |
|-------------------|--------|----------|-------------------------------------------------------------|
| `topicCode`       | string | Có       | Code snake_case của chủ đề (phải tồn tại trong hệ thống).  |
| `language`        | string | Không    | Mã ngôn ngữ. Mặc định `vi`.                                 |
| `level`           | enum   | Có       | `JUNIOR`, `MID`, hoặc `SENIOR`.                             |
| `content`         | string | Có       | Nội dung câu hỏi.                                           |
| `referenceAnswer` | string | Không    | Đáp án chuẩn.                                               |
| `keyPoints`       | string | Không    | Danh sách ý chính phân cách bởi `\|` (pipe).               |

### Định dạng JSON

```json
[
  {
    "topicCode": "oop",
    "language": "vi",
    "level": "JUNIOR",
    "content": "Giải thích OOP?",
    "referenceAnswer": "OOP bao gồm 4 nguyên tắc...",
    "keyPoints": [
      "Encapsulation: che giấu trạng thái nội bộ",
      "Inheritance: tái sử dụng code qua kế thừa"
    ]
  }
]
```

**Response 200 (AC-Q-04):**
```json
{
  "total": 10,
  "success": 8,
  "failed": 2,
  "errors": [
    { "row": 3, "message": "topicCode 'unknown_topic' không tồn tại trong hệ thống" },
    { "row": 7, "message": "Trường 'level' không hợp lệ: 'EXPERT'. Giá trị hợp lệ: JUNIOR, MID, SENIOR" }
  ]
}
```

| Trường    | Kiểu         | Mô tả                                                                      |
|-----------|--------------|----------------------------------------------------------------------------|
| `total`   | int          | Tổng số câu trong file.                                                    |
| `success` | int          | Số câu import thành công (trạng thái `DRAFT`).                             |
| `failed`  | int          | Số câu thất bại.                                                            |
| `errors`  | ErrorRow[]   | Chi tiết lỗi theo dòng. Không rollback toàn bộ khi một số dòng lỗi.       |

> Câu hỏi import thành công luôn ở trạng thái `DRAFT`; cần publish riêng để đưa vào pool RAG (FR-39).

**Response 400** (file rỗng hoặc định dạng không được hỗ trợ):
```json
{ "timestamp": "...", "status": 400, "message": "Chỉ hỗ trợ file .csv và .json", "errors": [] }
```

**Response 400** (vượt quá 200 câu):
```json
{ "timestamp": "...", "status": 400, "message": "File vượt quá giới hạn 200 câu hỏi mỗi lần import", "errors": [] }
```

---

## Quy tắc nghiệp vụ và vòng đời câu hỏi

### Vòng đời trạng thái

```
DRAFT ──publish──► PUBLISHED ──archive──► ARCHIVED
  ▲                    │
  │                    │ (sửa nội dung → cần reindex)
  └────────────────────┘
```

| Từ trạng thái | Hành động | Đến trạng thái | Điều kiện                   |
|---------------|-----------|----------------|-----------------------------|
| `DRAFT`       | publish   | `PUBLISHED`    | Phải đủ BR-19               |
| `PUBLISHED`   | archive   | `ARCHIVED`     | Luôn được phép              |
| `ARCHIVED`    | —         | —              | Không thể unarchive qua API |

### BR-19 — Điều kiện publish

Câu hỏi phải có **đủ 5 trường** trước khi publish:

| #  | Trường          | Điều kiện               |
|----|-----------------|-------------------------|
| 1  | `topicId`       | Khác null, tồn tại trong DB |
| 2  | `level`         | Một trong `JUNIOR`/`MID`/`SENIOR` |
| 3  | `content`       | Không trống             |
| 4  | `referenceAnswer` | Không trống           |
| 5  | `keyPoints`     | Có ít nhất 1 ý chính    |

Thiếu bất kỳ trường nào → HTTP 422 với danh sách `missingFields` (AC-Q-01 ≡ AC-18).

### AC-Q-02 — RAG pool

Câu hỏi chỉ được chọn vào pool RAG (dùng để sinh câu hỏi trong phiên phỏng vấn) khi **đồng thời** thỏa:
- `status = PUBLISHED`
- `indexStatus = INDEXED`

Câu hỏi `DRAFT`, `ARCHIVED`, hoặc `INDEX_FAILED` **không xuất hiện trong pool**.

### Indexing (FR-33-questions-index)

- Khi publish thành công, Java gọi `POST /ai/v1/index/questions` với payload câu hỏi.
- Python tạo embedding cho `content + "\n" + referenceAnswer` như một đơn vị và lưu vào `rag_question_vectors`.
- Nếu Python không khả dụng hoặc timeout (40s): câu hỏi vẫn `PUBLISHED` nhưng `indexStatus = INDEX_FAILED`; Admin thấy cảnh báo và có thể reindex thủ công qua `POST /{id}/reindex`.
- Mọi lời gọi `/ai/v1/index/*` đều ghi log vào `ai_call_logs` (AC-LOG-01, NFR-24).

### Giới hạn input (NFR bảo mật)

| Trường          | Giới hạn                    |
|-----------------|-----------------------------|
| `content`       | Tối đa 5 000 ký tự          |
| `referenceAnswer` | Không giới hạn cứng, khuyến nghị ≤ 10 000 ký tự |
| `keyPoints[].content` | Tối đa 1 000 ký tự mỗi ý chính |
| Import file     | Tối đa 200 câu / lần        |
