# API — System (Warm-up)

> Task 01 · UC-18 · NFR-31 · AC-23

## GET /api/v1/system/warmup

Không yêu cầu xác thực. Frontend gọi endpoint này khi mở trang để đánh thức backend.

**Request:** không có body, không cần header đặc biệt.

**Response 200:**
```json
{
  "status": "ok",
  "service": "backend",
  "timestamp": "2026-10-16T04:21:00.000Z"
}
```

**Ghi chú:** Thời gian warm-up không tính vào đồng hồ ứng viên (NFR-31). Trong Task 07 endpoint này sẽ gọi `/ai/v1/health` song song và trả thêm trạng thái AI service.

---

## GET /ai/v1/health (Python AI Service)

Không yêu cầu `X-Internal-Key` (để warm-up hoạt động khi Java chưa gửi được key).

**Response 200:**
```json
{
  "status": "ok",
  "service": "ai-service",
  "db": "ok",
  "embedding_provider": "mock"
}
```

**Response khi DB chưa sẵn sàng (cold start):**
```json
{
  "status": "degraded",
  "service": "ai-service",
  "db": "unavailable",
  "embedding_provider": "mock"
}
```

**Status `degraded`** không phải lỗi — Java retry cho đến khi `status=ok` (AC-23).
