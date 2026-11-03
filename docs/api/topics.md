# API — Quản lý chủ đề và alias (Admin Topics)

> Task 05 · M2 · FR-37 · NFR-27
> AC-TOPIC-01, AC-TOPIC-02, AC-TOPIC-03

## Tổng quan

- Các endpoint quản lý chủ đề (topic) dành cho **Admin** (FR-37): tạo, xem, cập nhật, xóa chủ đề, và quản lý alias (từ khóa đồng nghĩa) dùng cho JD mapping.
- Mỗi chủ đề có **một `code` duy nhất** dạng `snake_case`, **không đổi được** sau khi tạo (JPA `updatable = false`, không nằm trong request PUT/PATCH).
- Chủ đề hỗ trợ **đa ngôn ngữ** qua bảng `topic_translations` (NFR-27: bắt buộc ít nhất một bản dịch tiếng Việt `vi`; `en` tùy chọn). Ngôn ngữ là thuộc tính của nội dung, không hard-code.
- **Alias** là từ khóa đồng nghĩa giúp ánh xạ JD vào chủ đề (FR-37, BR-07). Ví dụ chủ đề `java_8_plus` có thể có alias `java 8`, `j8`.
- Hỗ trợ **1 cấp phân cấp** qua `parentId` (ví dụ `core_java` là con của `java_8_plus`).
- Tất cả route nằm dưới `/api/v1/admin/topics` → yêu cầu `ROLE_ADMIN` (FR-RBAC, NFR-19). Candidate gọi → **403**; thiếu/không hợp lệ JWT → **401**.
- AC-TOPIC-02: **Xóa bị chặn (409)** khi chủ đề có câu hỏi hoặc tài liệu liên kết → dùng PATCH `isActive=false` để vô hiệu hóa thay vì xóa.
- AC-TOPIC-03: PATCH với `{"isActive": false}` vô hiệu hóa chủ đề (deactivate) mà không xóa dữ liệu.

## Phân quyền (RBAC)

| Endpoint                                   | Quyền yêu cầu               | Không thỏa →     |
|--------------------------------------------|-----------------------------|------------------|
| `GET /api/v1/admin/topics`                 | `ROLE_ADMIN`                | Candidate → 403 (AC-RBAC-01), Không JWT → 401 (AC-RBAC-02) |
| `POST /api/v1/admin/topics`                | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `GET /api/v1/admin/topics/{id}`            | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `PUT /api/v1/admin/topics/{id}`            | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `PATCH /api/v1/admin/topics/{id}`          | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `DELETE /api/v1/admin/topics/{id}`         | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `GET /api/v1/admin/topics/{id}/aliases`    | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `POST /api/v1/admin/topics/{id}/aliases`   | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `DELETE /api/v1/admin/topics/{id}/aliases/{aliasId}` | `ROLE_ADMIN`      | Candidate → 403, Không JWT → 401 |

RoleHierarchy: `ADMIN > CANDIDATE` — Admin thừa hưởng quyền Candidate (FR-RBAC).

---

## Mô hình dữ liệu (Data models)

### TopicDto

Shape trả về bởi GET/POST/PUT/PATCH `/topics`:

```json
{
  "id": "9f1e...uuid",
  "code": "java_8_plus",
  "parentId": null,
  "isActive": true,
  "translations": [
    {
      "language": "vi",
      "name": "Java 8+",
      "description": "Java 8 trở lên"
    },
    {
      "language": "en",
      "name": "Java 8+",
      "description": "Java 8 and above"
    }
  ],
  "aliases": [
    { "id": "a1b2...uuid", "alias": "java 8" },
    { "id": "c3d4...uuid", "alias": "j8" }
  ]
}
```

| Trường          | Kiểu              | Mô tả                                                        |
|-----------------|-------------------|--------------------------------------------------------------|
| `id`            | UUID              | Id của chủ đề.                                               |
| `code`          | string            | Mã snake_case, duy nhất, không đổi sau khi tạo.             |
| `parentId`      | UUID \| null      | Id chủ đề cha (1 cấp phân cấp), `null` nếu là chủ đề gốc.    |
| `isActive`      | boolean           | `true` = đang hoạt động; `false` = đã vô hiệu hóa.           |
| `translations`  | TranslationDto[]  | Danh sách bản dịch theo ngôn ngữ. Bắt buộc ít nhất `vi`.     |
| `aliases`       | AliasDto[]        | Danh sách alias (từ khóa đồng nghĩa cho JD mapping).         |

### TranslationDto

Dùng trong response và làm đầu vào cho create/update/patch:

```json
{
  "language": "vi",
  "name": "Java 8+",
  "description": "Java 8 trở lên"
}
```

| Trường        | Kiểu   | Mô tả                                                          |
|---------------|--------|----------------------------------------------------------------|
| `language`    | string | Mã ngôn ngữ (VD: `vi`, `en`). Bắt buộc, không để trống.        |
| `name`        | string | Tên hiển thị của chủ đề theo ngôn ngữ. Bắt buộc, không để trống. |
| `description`| string | Mô tả tùy chọn theo ngôn ngữ.                                  |

### AliasDto

```json
{ "id": "a1b2...uuid", "alias": "java 8" }
```

| Trường  | Kiểu   | Mô tả                                  |
|---------|--------|----------------------------------------|
| `id`    | UUID   | Id của alias.                          |
| `alias` | string | Từ khóa đồng nghĩa của chủ đề (FR-37). |

### Error response (shape dùng chung)

Mọi lỗi trả về theo cùng shape:

```json
{ "timestamp": "...", "status": <mã HTTP>, "message": "<thông báo tiếng Việt>", "errors": [] }
```

---

## POST /api/v1/admin/topics

AC-TOPIC-01: tạo chủ đề mới với bản dịch và alias.

**Header:**
```
Authorization: Bearer <accessToken>
```
> `accessToken` phải có `role=ADMIN`.

**Request:**
```json
{
  "code": "java_8_plus",
  "parentId": null,
  "translations": [
    {
      "language": "vi",
      "name": "Java 8+",
      "description": "Java 8 trở lên"
    },
    {
      "language": "en",
      "name": "Java 8+",
      "description": "Java 8 and above"
    }
  ],
  "aliases": ["java 8", "j8"]
}
```

| Trường          | Kiểu             | Bắt buộc | Mô tả                                                |
|-----------------|------------------|----------|------------------------------------------------------|
| `code`          | string           | Có       | snake_case, regex `^[a-z][a-z0-9_]*$`, không đổi sau khi tạo. |
| `parentId`      | UUID             | Không    | Id chủ đề cha (1 cấp phân cấp). `null` nếu gốc.     |
| `translations`  | TranslationDto[] | Có       | Phải có ít nhất một bản dịch `vi` (NFR-27).          |
| `aliases`       | string[]         | Không    | Danh sách từ khóa đồng nghĩa. Bỏ qua phần tử trống. |

**Response 201:**
```json
{
  "id": "9f1e...uuid",
  "code": "java_8_plus",
  "parentId": null,
  "isActive": true,
  "translations": [
    { "language": "vi", "name": "Java 8+", "description": "Java 8 trở lên" },
    { "language": "en", "name": "Java 8+", "description": "Java 8 and above" }
  ],
  "aliases": [
    { "id": "a1b2...uuid", "alias": "java 8" },
    { "id": "c3d4...uuid", "alias": "j8" }
  ]
}
```

**Response 400** (code không phải snake_case):
```json
{ "timestamp": "...", "status": 400, "message": "Code phải là snake_case (chữ thường, số, gạch dưới)", "errors": [] }
```

**Response 400** (thiếu bản dịch tiếng Việt):
```json
{ "timestamp": "...", "status": 400, "message": "Phải có ít nhất một bản dịch tiếng Việt (vi)", "errors": [] }
```

**Response 409** (code đã tồn tại):
```json
{ "timestamp": "...", "status": 409, "message": "Chủ đề đã tồn tại với code này", "errors": [] }
```

**Response 404** (`parentId` chỉ đến chủ đề cha không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Chủ đề cha không tồn tại", "errors": [] }
```

---

## GET /api/v1/admin/topics

FR-37: liệt kê toàn bộ chủ đề (kèm bản dịch và alias).

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Response 200:**
```json
[
  {
    "id": "9f1e...uuid",
    "code": "java_8_plus",
    "parentId": null,
    "isActive": true,
    "translations": [
      { "language": "vi", "name": "Java 8+", "description": "Java 8 trở lên" }
    ],
    "aliases": [
      { "id": "a1b2...uuid", "alias": "java 8" }
    ]
  },
  {
    "id": "b2c3...uuid",
    "code": "core_java",
    "parentId": "9f1e...uuid",
    "isActive": true,
    "translations": [
      { "language": "vi", "name": "Java Core", "description": "Java căn bản" }
    ],
    "aliases": []
  }
]
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

## GET /api/v1/admin/topics/{id}

Lấy thông tin chi tiết một chủ đề theo id. Không tìm thấy → 404.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả              |
|---------|------|--------------------|
| `id`    | UUID | Id của chủ đề cần xem |

**Response 200:**
```json
{
  "id": "9f1e...uuid",
  "code": "java_8_plus",
  "parentId": null,
  "isActive": true,
  "translations": [
    { "language": "vi", "name": "Java 8+", "description": "Java 8 trở lên" },
    { "language": "en", "name": "Java 8+", "description": "Java 8 and above" }
  ],
  "aliases": [
    { "id": "a1b2...uuid", "alias": "java 8" }
  ]
}
```

**Response 404** (chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

---

## PUT /api/v1/admin/topics/{id}

Cập nhật toàn phần (full update). `code` là **bất biến** — không nằm trong request (JPA `updatable = false`).

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                |
|---------|------|----------------------|
| `id`    | UUID | Id của chủ đề cần cập nhật |

**Request:**
```json
{
  "parentId": "9f1e...uuid",
  "translations": [
    { "language": "vi", "name": "Java Core", "description": "Java căn bản" },
    { "language": "en", "name": "Java Core", "description": "Core Java" }
  ],
  "aliases": ["core java", "java core"]
}
```

| Trường          | Kiểu             | Bắt buộc | Mô tả                                                |
|-----------------|------------------|----------|------------------------------------------------------|
| `parentId`      | UUID             | Không    | Id chủ đề cha; `null` để bỏ liên kết cha.            |
| `translations`  | TranslationDto[] | Không    | Thay thế toàn bộ bản dịch nếu truyền (clear + thêm).  |
| `aliases`        | string[]         | Không    | Thay thế toàn bộ alias nếu truyền (clear + thêm).     |

**Response 200:**
```json
{
  "id": "b2c3...uuid",
  "code": "core_java",
  "parentId": "9f1e...uuid",
  "isActive": true,
  "translations": [
    { "language": "vi", "name": "Java Core", "description": "Java căn bản" },
    { "language": "en", "name": "Java Core", "description": "Core Java" }
  ],
  "aliases": [
    { "id": "d4e5...uuid", "alias": "core java" },
    { "id": "e5f6...uuid", "alias": "java core" }
  ]
}
```

**Response 404** (chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

**Response 404** (`parentId` chỉ đến chủ đề cha không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Chủ đề cha không tồn tại", "errors": [] }
```

---

## PATCH /api/v1/admin/topics/{id}

Cập nhật một phần (partial update). Chỉ các trường **không null** được áp dụng.
AC-TOPIC-03: dùng `{"isActive": false}` để vô hiệu hóa chủ đề (deactivate) thay vì xóa.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                |
|---------|------|----------------------|
| `id`    | UUID | Id của chủ đề cần cập nhật |

**Request:**
```json
{ "isActive": false }
```

| Trường          | Kiểu             | Bắt buộc | Mô tả                                                    |
|-----------------|------------------|----------|----------------------------------------------------------|
| `parentId`      | UUID             | Không    | Id chủ đề cha (chỉ áp dụng khi khác null).               |
| `isActive`      | Boolean          | Không    | `true` = kích hoạt; `false` = vô hiệu hóa (AC-TOPIC-03).  |
| `translations`  | TranslationDto[] | Không    | Thay thế toàn bộ bản dịch nếu truyền (clear + thêm).      |
| `aliases`        | string[]         | Không    | Thay thế toàn bộ alias nếu truyền (clear + thêm).         |

**Vô hiệu hóa chủ đề (deactivate):**
```json
{ "isActive": false }
```

**Cập nhật bản dịch + alias:**
```json
{
  "translations": [
    { "language": "vi", "name": "Java 8+", "description": "Java 8 trở lên" }
  ],
  "aliases": ["java 8", "j8"]
}
```

**Response 200:**
```json
{
  "id": "9f1e...uuid",
  "code": "java_8_plus",
  "parentId": null,
  "isActive": false,
  "translations": [
    { "language": "vi", "name": "Java 8+", "description": "Java 8 trở lên" }
  ],
  "aliases": [
    { "id": "a1b2...uuid", "alias": "java 8" }
  ]
}
```

**Response 404** (chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

**Response 404** (`parentId` chỉ đến chủ đề cha không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Chủ đề cha không tồn tại", "errors": [] }
```

---

## DELETE /api/v1/admin/topics/{id}

AC-TOPIC-02: xóa chủ đề. **Bị chặn (409)** khi chủ đề có câu hỏi hoặc tài liệu liên kết.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả              |
|---------|------|--------------------|
| `id`    | UUID | Id của chủ đề cần xóa |

**Response 204:**
> Không có body. Trả về khi xóa thành công.

**Response 404** (chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

**Response 409** (chủ đề có câu hỏi hoặc tài liệu liên kết):
```json
{ "timestamp": "...", "status": 409, "message": "Không thể xóa chủ đề khi có câu hỏi hoặc tài liệu liên kết", "errors": [] }
```

### Hành vi khi xóa bị chặn

Khi chủ đề đã có câu hỏi hoặc tài liệu liên kết, server trả **409** thay vì xóa. Giải pháp thay thế là **vô hiệu hóa** chủ đề qua PATCH:

```http
PATCH /api/v1/admin/topics/{id}
Content-Type: application/json

{ "isActive": false }
```

> Lý do dùng deactivate: chủ đề đã được tham chiếu bởi câu hỏi/tài liệu; xóa sẽ làm gãy dữ liệu. Vô hiệu hóa giữ nguyên liên kết nhưng ẩn chủ đề khỏi danh sách hoạt động (AC-TOPIC-02, AC-TOPIC-03).

---

## GET /api/v1/admin/topics/{id}/aliases

FR-37: liệt kê alias của một chủ đề.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả              |
|---------|------|--------------------|
| `id`    | UUID | Id của chủ đề cần xem alias |

**Response 200:**
```json
[
  { "id": "a1b2...uuid", "alias": "java 8" },
  { "id": "c3d4...uuid", "alias": "j8" }
]
```

**Response 404** (chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

---

## POST /api/v1/admin/topics/{id}/aliases

Thêm alias (từ khóa đồng nghĩa) cho chủ đề. Alias trùng (không phân biệt hoa thường) → 409.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả              |
|---------|------|--------------------|
| `id`    | UUID | Id của chủ đề cần thêm alias |

**Request:**
```json
{ "alias": "java 8" }
```

| Trường  | Kiểu   | Bắt buộc | Mô tả                              |
|---------|--------|----------|------------------------------------|
| `alias` | string | Có       | Từ khóa đồng nghĩa, không để trống. |

**Response 201:**
```json
{ "id": "a1b2...uuid", "alias": "java 8" }
```

**Response 409** (alias đã tồn tại cho chủ đề này):
```json
{ "timestamp": "...", "status": 409, "message": "Alias đã tồn tại cho chủ đề này", "errors": [] }
```

**Response 404** (chủ đề không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy chủ đề", "errors": [] }
```

---

## DELETE /api/v1/admin/topics/{id}/aliases/{aliasId}

Xóa alias khỏi chủ đề. Alias không thuộc chủ đề → 404.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số    | Kiểu | Mô tả                       |
|------------|------|-----------------------------|
| `id`       | UUID | Id của chủ đề sở hữu alias. |
| `aliasId`  | UUID | Id của alias cần xóa.      |

**Response 204:**
> Không có body. Trả về khi xóa thành công.

**Response 404** (chủ đề hoặc alias không tồn tại, hoặc alias không thuộc chủ đề):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy alias", "errors": [] }
```

---

## Cấu hình và quy tắc nghiệp vụ

### Quy tắc code

- `code` là **snake_case**: regex `^[a-z][a-z0-9_]*$` (bắt đầu bằng chữ thường, tiếp theo là chữ thường/số/gạch dưới).
- `code` **duy nhất** (không phân biệt hoa thường khi kiểm tra trùng).
- `code` **không đổi** sau khi tạo (JPA `updatable = false`; không nằm trong request PUT/PATCH).

### Đa ngôn ngữ (NFR-27)

- Bắt buộc ít nhất một bản dịch **`vi`** khi tạo chủ đề.
- Ngôn ngữ là thuộc tính của nội dung, không hard-code — thêm `en` tùy chọn.
- PUT/PATCH thay thế toàn bộ bản dịch nếu trường `translations` được truyền (clear + thêm lại).

### Phân cấp 1 cấp

- `parentId` tạo quan hệ cha–con 1 cấp (ví dụ `core_java` là con của `java_8_plus`).
- `parentId` = `null` cho chủ đề gốc.
- Chủ đề cha không tồn tại → 404.

### Alias (BR-07, FR-37)

- Alias là từ khóa đồng nghĩa giúp ánh xạ JD vào chủ đề.
- Alias trùng (không phân biệt hoa thường) trong cùng chủ đề → 409.

### Xóa vs vô hiệu hóa

| Tình huống                                  | Khuyến nghị              |
|---------------------------------------------|--------------------------|
| Chủ đề chưa có câu hỏi/tài liệu liên kết     | DELETE → 204             |
| Chủ đề đã có câu hỏi/tài liệu liên kết       | PATCH `{"isActive": false}` → 200 |
| Muốn giữ dữ liệu nhưng ẩn chủ đề            | PATCH `{"isActive": false}` |
