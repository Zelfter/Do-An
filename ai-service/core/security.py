"""
Internal API key authentication for /ai/v1 endpoints.
Java backend must send X-Internal-Key header on every request. (design.md §6)
"""
from typing import Optional
from fastapi import Header, HTTPException, status
from core.config import settings


async def verify_internal_key(x_internal_key: Optional[str] = Header(default=None)) -> None:
    """
    FastAPI dependency: validate X-Internal-Key header.
    Missing or invalid key → 401 Unauthorized (design.md §6).
    Using Optional[str] with default=None instead of ... so that FastAPI
    returns 401 (not 422) when the header is absent.
    """
    if x_internal_key is None or x_internal_key != settings.internal_api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid internal API key",
        )
