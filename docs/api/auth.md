# API — Xác thực và phân quyền (Auth + RBAC)

> Task 03 · M1 · FR-01, FR-01-JWT, FR-RBAC · NFR-17, NFR-19
> AC-AUTH-01..07, AC-RBAC-01..02

## Tổng quan

- Mật khẩu băm bằng **BCrypt** (cost 12, NFR-17). Mật khẩu **không bao giờ** xuất hiện trong bất kỳ response nào (AC-AUTH-01, NFR-17).
- Access token là **JWT HS256** với payload `{sub, role, iat, exp}`. TTL lấy từ `JWT_ACCESS_TTL_MINUTES` (mặc định 60 phút).
- Refresh token là **opaque token**; chỉ lưu `SHA-256(raw_token)` trong DB. Raw token chỉ trả về client **một lần**. TTL lấy từ `JWT_REFRESH_TTL_DAYS` (mặc định 7 ngày).
- JWT signing key chỉ đọc từ biến môi trường `JWT_SECRET` (≥32 byte), không bao giờ hard-code (NFR-17).
- Người đăng ký mới mặc định `role=CANDIDATE`, `status=ACTIVE`.
- Tài khoản `LOCKED`: đăng nhập trả **403**; mọi refresh token của user bị thu hồi (AC-AUTH-07, FR-34).

## Phân quyền (RBAC)

| Endpoint            | Quyền yêu cầu                | Không thỏa →     |
|---------------------|------------------------------|------------------|
| `/api/v1/auth/**`   | công khai (permitAll)         | —                |
| `/api/v1/system/warmup` | công khai (permitAll)      | —                |
| `/api/v1/admin/**`  | `ROLE_ADMIN`                 | Candidate → 403 (AC-RBAC-01) |
| `/api/v1/**`        | đã xác thực (JWT)            | Không JWT → 401 (AC-RBAC-02) |
| `/ai/v1/**`         | nội bộ, kiểm tra `X-Internal-Key` (không qua JWT) | — |

RoleHierarchy: `ADMIN > CANDIDATE` — Admin có thể xem giao diện Candidate (FR-RBAC).

> Lưu ý: access token là stateless — filter không truy vấn DB mỗi request. Việc khóa tài khoản được thực thi tại login (403) và thu hồi refresh token; access token đã cấp vẫn dùng được đến khi hết hạn.

---

## POST /api/v1/auth/register

AC-AUTH-01 / AC-AUTH-02. Người đăng ký mới luôn là CANDIDATE + ACTIVE.

**Request:**
```json
{
  "email": "candidate@example.com",
  "password": "Password123!"
}
```
- `email`: RFC 5322.
- `password`: tối thiểu 8 ký tự.

**Response 201:**
```json
{
  "id": "9f1e...uuid",
  "email": "candidate@example.com",
  "role": "CANDIDATE",
  "status": "ACTIVE"
}
```
> Response không chứa `password`/`password_hash` (AC-AUTH-01, NFR-17).

**Response 409** (email đã tồn tại):
```json
{ "timestamp": "...", "status": 409, "message": "Email đã tồn tại trong hệ thống", "errors": [] }
```

---

## POST /api/v1/auth/login

AC-AUTH-03 / AC-AUTH-04 / AC-AUTH-07.

**Request:**
```json
{
  "email": "candidate@example.com",
  "password": "Password123!"
}
```

**Response 200:**
```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<opaque>",
  "expiresIn": 3600
}
```
- `accessToken`: JWT HS256, payload `{sub, role, iat, exp}`.
- `refreshToken`: opaque, gửi cho client đúng một lần.
- `expiresIn`: thời gian sống access token tính bằng giây.

**Response 401** (sai email/mật khẩu — cùng message để tránh enumeration):
```json
{ "timestamp": "...", "status": 401, "message": "Email hoặc mật khẩu không đúng", "errors": [] }
```

**Response 403** (tài khoản bị LOCKED, credentials đúng):
```json
{ "timestamp": "...", "status": 403, "message": "Tài khoản đã bị khóa", "errors": [] }
```

---

## POST /api/v1/auth/refresh

AC-AUTH-05 / AC-AUTH-06. Đổi refresh token hợp lệ lấy access token mới.

**Request:**
```json
{ "refreshToken": "<opaque>" }
```

**Response 200:**
```json
{ "accessToken": "<JWT mới>", "expiresIn": 3600 }
```

**Response 401** (token không hợp lệ / đã thu hồi / hết hạn):
```json
{ "timestamp": "...", "status": 401, "message": "Refresh token ...", "errors": [] }
```

---

## POST /api/v1/auth/logout

Server thu hồi refresh token; client tự xóa cả access và refresh token.

**Request:**
```json
{ "refreshToken": "<opaque>" }
```

**Response 204** (idempotent — trả 204 kể cả khi token không còn tồn tại).

---

## GET /api/v1/me

Yêu cầu JWT (`/api/v1/**` → authenticated).

**Header:**
```
Authorization: Bearer <accessToken>
```

**Response 200:**
```json
{
  "id": "9f1e...uuid",
  "email": "candidate@example.com",
  "role": "CANDIDATE",
  "status": "ACTIVE"
}
```

**Response 401** (thiếu hoặc JWT không hợp lệ).

---

## Cấu hình biến môi trường

| Biến                     | Mặc định | Mô tả |
|--------------------------|----------|-------|
| `JWT_SECRET`             | (bắt buộc trong prod) | Khóa ký HS256, ≥32 byte. Không bao giờ hard-code. |
| `JWT_ACCESS_TTL_MINUTES`  | `60`     | TTL access token (phút). |
| `JWT_REFRESH_TTL_DAYS`    | `7`      | TTL refresh token (ngày). |
| `ALLOWED_ORIGINS`         | `http://localhost:5173` | CORS allowlist, phân tách bởi dấu phẩy. |
