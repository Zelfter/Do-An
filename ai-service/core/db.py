"""
Async PostgreSQL connection pool for Neon.
Java owns schema `app`; Python owns schema `rag`. (SRS §10.5)
No cross-schema foreign keys.
"""
import logging
import socket
from contextlib import asynccontextmanager
from urllib.parse import urlparse
from typing import AsyncGenerator

import psycopg
from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool

from core.config import settings

log = logging.getLogger(__name__)

# Module-level pool — created on startup, closed on shutdown
_pool: AsyncConnectionPool | None = None


async def init_pool() -> None:
    """Initialize connection pool. Called from FastAPI lifespan."""
    global _pool
    log.info("Initialising async DB pool → rag schema")

    # Resolve hostname to IPv4 to avoid IPv6 timeout issues on some networks.
    # Neon endpoints resolve to both IPv4 and IPv6; if IPv6 is unreachable the
    # pool will hang until connect_timeout expires on each IPv6 address.
    pool_kwargs: dict = {"row_factory": dict_row}
    try:
        parsed = urlparse(settings.database_url)
        hostname = parsed.hostname
        if hostname:
            infos = socket.getaddrinfo(hostname, 5432, socket.AF_INET)
            if infos:
                ipv4 = infos[0][4][0]
                pool_kwargs["hostaddr"] = ipv4
                log.info("Resolved %s → IPv4 %s (avoiding IPv6 timeout)", hostname, ipv4)
    except Exception as exc:
        log.warning("IPv4 resolution failed, falling back to default: %s", exc)

    _pool = AsyncConnectionPool(
        conninfo=settings.database_url,
        min_size=1,
        max_size=5,  # NFR-30: small pool for 0.1 vCPU
        kwargs=pool_kwargs,
        open=False,
    )
    await _pool.open()
    log.info("DB pool ready")


async def close_pool() -> None:
    """Close pool on shutdown."""
    global _pool
    if _pool:
        await _pool.close()
        log.info("DB pool closed")


@asynccontextmanager
async def get_conn() -> AsyncGenerator[psycopg.AsyncConnection, None]:
    """Context manager: yield a connection from the pool."""
    if _pool is None:
        raise RuntimeError("DB pool not initialised — call init_pool() first")
    async with _pool.connection() as conn:
        yield conn
