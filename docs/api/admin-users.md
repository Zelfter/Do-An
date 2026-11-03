# API — Quản lý người dùng (Admin Users)

> Task 04 · M1 · FR-34 · NFR-19
> AC-USER-01, AC-USER-02

## Tổng quan

- Các endpoint quản lý người dùng dành cho **Admin** (FR-34): xem danh sách, xem chi tiết và thay đổi trạng thái tài khoản (`ACTIVE` ↔ `LOCKED`).
- Tất cả route nằm dưới `/api/v1/admin/**` → yêu cầu `ROLE_ADMIN` (FR-RBAC, NFR-19). Candidate gọi → **403**; thiếu/không hợp lệ JWT → **401**.
- Mọi response đều dùng `UserDto` với shape `{id, email, role, status}` — **không bao giờ** chứa `password` hay `password_hash` (NFR-17, AC-AUTH-01).
- Khi khóa tài khoản (`status=LOCKED`): toàn bộ refresh token của user bị thu hồi ngay lập tức qua bulk UPDATE, các phiên đang hoạt động không thể refresh nữa (AC-USER-01, FR-34). Access token đã cấp vẫn dùng được đến khi hết hạn (stateless).
- Admin **không thể** tự khóa tài khoản của chính mình — trả **400** (AC-USER-02).
- `id` của admin hiện tại lấy từ JWT `sub` claim (do `JwtAuthFilter` đặt làm principal trong `SecurityContext`).

## Phân quyền (RBAC)

| Endpoint                          | Quyền yêu cầu               | Không thỏa →     |
|-----------------------------------|-----------------------------|------------------|
| `GET /api/v1/admin/users`         | `ROLE_ADMIN`                | Candidate → 403 (AC-RBAC-01), Không JWT → 401 (AC-RBAC-02) |
| `GET /api/v1/admin/users/{id}`    | `ROLE_ADMIN`                | Candidate → 403, Không JWT → 401 |
| `PATCH /api/v1/admin/users/{id}/status` | `ROLE_ADMIN`          | Candidate → 403, Không JWT → 401 |

RoleHierarchy: `ADMIN > CANDIDATE` — Admin thừa hưởng quyền Candidate (FR-RBAC).

---

## GET /api/v1/admin/users

FR-34: Admin xem danh sách users với filter theo `role` và `status`. Khi không truyền filter nào, trả toàn bộ user.

**Header:**
```
Authorization: Bearer <accessToken>
```
> `accessToken` phải có `role=ADMIN`.

**Query params (tùy chọn):**

| Tham số   | Kiểu   | Mô tả                                  | Ví dụ       |
|-----------|--------|----------------------------------------|-------------|
| `role`    | string | Lọc theo vai trò (không phân biệt hoa thường) | `CANDIDATE` |
| `status`  | string | Lọc theo trạng thái (không phân biệt hoa thường) | `LOCKED`    |

- Có thể dùng một trong hai, cả hai, hoặc không dùng filter nào.
- Khi cả hai cùng truyền → lọc theo `role` VÀ `status`.

**Response 200:**
```json
[
  {
    "id": "9f1e...uuid",
    "email": "candidate@example.com",
    "role": "CANDIDATE",
    "status": "ACTIVE"
  },
  {
    "id": "a2b3...uuid",
    "email": "admin@example.com",
    "role": "ADMIN",
    "status": "ACTIVE"
  }
]
```
> Mỗi phần tử là `UserDto` — không chứa `password_hash` (NFR-17).

**Response 401** (thiếu hoặc JWT không hợp lệ):
```json
{ "timestamp": "...", "status": 401, "message": "...", "errors": [] }
```

**Response 403** (Candidate JWT):
```json
{ "timestamp": "...", "status": 403, "message": "...", "errors": [] }
```

---

## GET /api/v1/admin/users/{id}

Lấy thông tin chi tiết của một user theo id. Không tìm thấy → 404.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả          |
|---------|------|----------------|
| `id`    | UUID | Id của user cần xem |

**Response 200:**
```json
{
  "id": "9f1e...uuid",
  "email": "candidate@example.com",
  "role": "CANDIDATE",
  "status": "ACTIVE"
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

**Response 404** (user không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy người dùng", "errors": [] }
```

---

## PATCH /api/v1/admin/users/{id}/status

AC-USER-01 / AC-USER-02 / FR-34. Thay đổi trạng thái tài khoản giữa `ACTIVE` và `LOCKED`.

**Header:**
```
Authorization: Bearer <accessToken (role=ADMIN)>
```

**Path param:**

| Tham số | Kiểu | Mô tả                        |
|---------|------|------------------------------|
| `id`    | UUID | Id của user cần đổi trạng thái |

**Request body:**
```json
{ "status": "LOCKED" }
```
- `status` bắt buộc, không được để trống. Chỉ chấp nhận `ACTIVE` hoặc `LOCKED` (không phân biệt hoa thường, sẽ được chuẩn hóa thành hoa).

**Response 200:**
```json
{
  "id": "9f1e...uuid",
  "email": "candidate@example.com",
  "role": "CANDIDATE",
  "status": "LOCKED"
}
```

**Response 400** (giá trị status không hợp lệ):
```json
{ "timestamp": "...", "status": 400, "message": "Trạng thái không hợp lệ, chỉ chấp nhận ACTIVE hoặc LOCKED", "errors": [] }
```

**Response 400** (status để trống/thiếu):
```json
{ "timestamp": "...", "status": 400, "message": "Trạng thái không được để trống", "errors": [] }
```

**Response 400** (Admin tự khóa chính mình — AC-USER-02):
```json
{ "timestamp": "...", "status": 400, "message": "Không thể khóa tài khoản của chính mình", "errors": [] }
```
> Kiểm tra: nếu `id` trùng với id của admin hiện tại (lấy từ JWT `sub`) VÀ `status=LOCKED` → 400.

**Response 401** (thiếu hoặc JWT không hợp lệ):
```json
{ "timestamp": "...", "status": 401, "message": "...", "errors": [] }
```

**Response 403** (Candidate JWT):
```json
{ "timestamp": "...", "status": 403, "message": "...", "errors": [] }
```

**Response 404** (user không tồn tại):
```json
{ "timestamp": "...", "status": 404, "message": "Không tìm thấy người dùng", "errors": [] }
```

### Hành vi khi khóa tài khoản (LOCKED)

Khi `status` chuyển sang `LOCKED`, server gọi `revokeAllTokensForUser(id)` — thực hiện **bulk UPDATE** thu hồi toàn bộ refresh token của user đó ngay lập tức (AC-USER-01, FR-34):

- Các phiên đang hoạt động **không thể refresh** access token nữa → buộc đăng nhập lại.
- Tuy nhiên, access token đã cấp **vẫn dùng được đến khi hết hạn** (stateless, filter không truy vấn DB mỗi request). Việc khóa được thực thi tại login (trả 403) và thu hồi refresh token.
- Tài khoản `LOCKED` khi đăng nhập sẽ trả **403** (xem `docs/api/auth.md` — POST /auth/login).

### Chặn tự khóa (AC-USER-02)

Admin không thể đặt `status=LOCKED` cho chính tài khoản của mình. Id của admin hiện tại được lấy từ JWT `sub` claim (do `JwtAuthFilter` đặt làm `@AuthenticationPrincipal UUID`). Nếu `targetUserId == currentUserId` và `status=LOCKED` → trả 400 với message "Không thể khóa tài khoản của chính mình". Mở khóa (ACTIVE) chính mình thì được phép.

---

## Cấu hình biến môi trường

Các biến này được chia sẻ với module Auth (xem `docs/api/auth.md`):

| Biến                     | Mặc định | Mô tả |
|--------------------------|----------|-------|
| `JWT_SECRET`             | (bắt buộc trong prod) | Khóa ký HS256, ≥32 byte. |
| `JWT_ACCESS_TTL_MINUTES`  | `60`     | TTL access token (phút) — ảnh hưởng thời gian access token vẫn dùng được sau khi user bị khóa. |
| `JWT_REFRESH_TTL_DAYS`    | `7`      | TTL refresh token (ngày) — bị thu hồi ngay khi user bị khóa. |
