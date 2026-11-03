# Ôn Java & Phỏng vấn mô phỏng theo JD

> Ứng dụng web giúp ôn kiến thức Java và luyện phỏng vấn bám theo JD.
> SRS: `docs/source/SRS-extracted.md` (bản trích; DOCX gốc là tài liệu chuẩn).

## Kiến trúc

```
frontend/     React 18 + Vite → Vercel
backend/      Spring Boot 3, Java 21 → Render
ai-service/   FastAPI Python → Koyeb
```

Xem chi tiết: `docs/architecture/overview.md`

---

## Khởi động môi trường phát triển

### Yêu cầu
- Java 21 + Maven 3.9+
- Python 3.12+ + pip
- Node.js 20+
- Tài khoản Neon (tạo dev branch, copy connection string)

### 1. Biến môi trường

Tạo file `.env` trong thư mục gốc (không commit):
```bash
# Tham khảo .env.example
cp .env.example .env
# Điền DATABASE_URL, JWT_SECRET, INTERNAL_API_KEY, ...
```

### 2. Java Backend

```bash
cd backend
# Lần đầu (tải dependencies ~2 phút):
mvn dependency:resolve -q
# Khởi động:
mvn spring-boot:run
# → http://localhost:8080
# → http://localhost:8080/api/v1/system/warmup
```

> JVM options (NFR-30): `-Xms64m -Xmx320m -XX:+UseSerialGC` — xem `pom.xml`

### 3. Python AI Service

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
# → http://localhost:8000/ai/v1/health
# → http://localhost:8000/docs (chỉ khi EMBEDDING_PROVIDER=mock)
```

> Không dùng torch, sentence-transformers (NFR-06, NFR-30)
> Mặc định: `EMBEDDING_PROVIDER=mock` — không tốn quota thật

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
# Proxy /api/v1 → localhost:8080 (vite.config.ts)
```

### 5. Chạy test

```bash
# Java unit tests (H2 in-memory, không cần Neon):
cd backend && mvn test

# Python tests:
cd ai-service && pytest tests/ -v

# Frontend:
cd frontend && npm test
```

---

## Spec đang triển khai

| Spec | Trạng thái | Tasks |
|---|---|---|
| 01-foundation-content | 🔄 Task 01 Done | Task 02–08 còn lại |

Xem `.kiro/specs/01-foundation-content/tasks.md`

---

## Tài liệu

- `docs/source/SRS-extracted.md` — SRS trích xuất (không sửa)
- `docs/architecture/overview.md` — Sơ đồ kiến trúc
- `docs/decisions/open-questions.md` — Quyết định cần xác nhận
- `docs/traceability.csv` — RTM đầy đủ

---

> ⚠️ Secrets (JWT_SECRET, API keys, DB passwords) chỉ đặt trong biến môi trường.
> Không bao giờ commit vào git. (design.md §6)
