"""
Simple migration runner for schema `rag`.
Python owns schema `rag`; Java owns schema `app` (Flyway). (SRS §10.5)
Runs SQL files in migrations/ directory on startup.
"""
import logging
from pathlib import Path

log = logging.getLogger(__name__)

MIGRATIONS_DIR = Path(__file__).parent.parent / "migrations"


async def run_migrations() -> None:
    """Execute rag schema migrations in alphabetical order (idempotent SQL)."""
    from core.db import get_conn

    migration_files = sorted(MIGRATIONS_DIR.glob("*.sql"))
    if not migration_files:
        log.warning("No migration files found in %s", MIGRATIONS_DIR)
        return

    async with get_conn() as conn:
        for sql_file in migration_files:
            log.info("Running migration: %s", sql_file.name)
            sql = sql_file.read_text(encoding="utf-8")
            try:
                await conn.execute(sql)
                await conn.commit()
                log.info("Migration OK: %s", sql_file.name)
            except Exception as exc:
                await conn.rollback()
                log.error("Migration FAILED: %s — %s", sql_file.name, exc)
                raise


if __name__ == "__main__":
    """CLI entry point: run `rag` schema migrations standalone.

    Usage (from ai-service/):
        DATABASE_URL=postgresql://postgres:postgres@localhost:5432/interview_dev \
            .venv/bin/python -m core.migrations

    This creates a connection pool, runs all migration SQL files in
    `migrations/` in alphabetical order, then closes the pool.
    Mirrors the startup-path used by the FastAPI lifespan (design.md §5.2).
    """
    import asyncio
    import logging

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )

    async def _main() -> None:
        from core.db import init_pool, close_pool

        await init_pool()
        try:
            await run_migrations()
        finally:
            await close_pool()

    asyncio.run(_main())
