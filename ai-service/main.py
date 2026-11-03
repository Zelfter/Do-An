"""
Python AI Service — FastAPI stateless service.

Responsibilities (SRS §10.1, architecture-guardrails.md):
  ✅ Owns schema `rag` (chunking, embedding, pgvector)
  ✅ JD analysis, answer evaluation, follow-up generation (Tasks 07+)
  ✅ LLM/embedding provider adapter (NFR-07)
  ✅ Validates JSON output before returning to Java

Does NOT:
  ❌ Own interview session state
  ❌ Make final business-rule decisions (follow-up eligibility, scoring)
  ❌ Import torch / sentence-transformers / transformers (NFR-06, NFR-30)
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routes import health, index
from core.config import settings
from core.db import init_pool, close_pool
from core.migrations import run_migrations

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup: initialise DB pool and run `rag` schema migrations.
    Shutdown: close the pool cleanly.

    Per design.md §5.2, Python runs schema `rag` migrations on startup
    (startup event) or via a separate command. The migration SQL uses
    `CREATE ... IF NOT EXISTS`, so running on every startup is idempotent
    and safe.
    """
    log.info("AI Service starting — provider=%s", settings.embedding_provider)
    await init_pool()
    await run_migrations()
    yield
    await close_pool()
    log.info("AI Service stopped")


app = FastAPI(
    title="Interview AI Service",
    description=(
        "Internal Python service for RAG indexing, JD analysis, "
        "answer evaluation and follow-up generation. "
        "All endpoints require X-Internal-Key header (design.md §6)."
    ),
    version="0.1.0",
    lifespan=lifespan,
    # Disable Swagger in production to reduce attack surface
    docs_url="/docs" if settings.embedding_provider == "mock" else None,
    redoc_url=None,
)

# CORS: this service is only called by Java backend (internal),
# but configure permissive localhost CORS for local dev convenience.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Routers ---
# /ai/v1/health — UC-18, NFR-31 (no auth for warm-up)
app.include_router(health.router, prefix="/ai/v1")

# /ai/v1/index/* — FR-33, FR-33-questions-index
# X-Internal-Key auth is applied per-endpoint in index.py (design §6)
app.include_router(index.router, prefix="/ai/v1")

# Future routers (Tasks 08+):
# app.include_router(evaluate.router, prefix="/ai/v1", dependencies=[Depends(verify_internal_key)])
# app.include_router(jd.router, prefix="/ai/v1", dependencies=[Depends(verify_internal_key)])
# app.include_router(report.router, prefix="/ai/v1", dependencies=[Depends(verify_internal_key)])
