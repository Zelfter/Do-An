"""
Health endpoint — GET /ai/v1/health
Used by Java backend warm-up (UC-18, NFR-31, AC-23).
No authentication required so warm-up can proceed before JWT is available.
"""
import logging
from typing import Any

from fastapi import APIRouter
from core.db import get_conn
from core.config import settings

log = logging.getLogger(__name__)
router = APIRouter()


@router.get("/health", summary="Service health check (warm-up)")
async def health() -> dict[str, Any]:
    """
    Returns service status, DB reachability and configured provider.
    Java polls this endpoint during warm-up to know when Python is ready.
    (UC-18, NFR-31, AC-23)
    """
    db_ok = await _check_db()
    return {
        "status": "ok" if db_ok else "degraded",
        "service": "ai-service",
        "db": "ok" if db_ok else "unavailable",
        "embedding_provider": settings.embedding_provider,
    }


async def _check_db() -> bool:
    try:
        async with get_conn() as conn:
            await conn.execute("SELECT 1")
        return True
    except Exception as exc:
        log.warning("DB health check failed: %s", exc)
        return False
