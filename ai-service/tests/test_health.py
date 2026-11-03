"""
Task 01 — Health endpoint test (UC-18, NFR-31, AC-23).
Uses FastAPI TestClient; mocks DB and psycopg to avoid needing real Neon in CI.
NFR-33: no real API calls, no real DB in unit tests.
"""
import sys
import types
import pytest
from unittest.mock import AsyncMock, MagicMock, patch


def _install_psycopg_stub():
    """
    Stub out psycopg and psycopg_pool so core/db.py can be imported
    without the native psycopg binary installed.
    """
    if "psycopg" not in sys.modules:
        psycopg_stub = types.ModuleType("psycopg")
        psycopg_stub.AsyncConnection = MagicMock
        psycopg_stub.rows = types.ModuleType("psycopg.rows")
        psycopg_stub.rows.dict_row = MagicMock()
        sys.modules["psycopg"] = psycopg_stub
        sys.modules["psycopg.rows"] = psycopg_stub.rows

    if "psycopg_pool" not in sys.modules:
        pool_stub = types.ModuleType("psycopg_pool")
        pool_stub.AsyncConnectionPool = MagicMock
        sys.modules["psycopg_pool"] = pool_stub


_install_psycopg_stub()


@pytest.fixture
def client():
    """TestClient with DB pool and migrations mocked — no real Neon needed."""
    with patch("core.db.init_pool", new_callable=AsyncMock), \
         patch("core.db.close_pool", new_callable=AsyncMock), \
         patch("main.run_migrations", new_callable=AsyncMock):
        from main import app
        from fastapi.testclient import TestClient
        with TestClient(app) as c:
            yield c


def test_health_returns_200(client):
    """DoD: GET /ai/v1/health → 200 when service is up."""
    with patch("api.routes.health._check_db", new_callable=AsyncMock, return_value=True):
        response = client.get("/ai/v1/health")
    assert response.status_code == 200


def test_health_status_ok_when_db_up(client):
    """status=ok when DB reachable."""
    with patch("api.routes.health._check_db", new_callable=AsyncMock, return_value=True):
        response = client.get("/ai/v1/health")
    data = response.json()
    assert data["status"] == "ok"
    assert data["db"] == "ok"
    assert data["service"] == "ai-service"


def test_health_status_degraded_when_db_down(client):
    """status=degraded when DB unreachable (cold start scenario — AC-23)."""
    with patch("api.routes.health._check_db", new_callable=AsyncMock, return_value=False):
        response = client.get("/ai/v1/health")
    data = response.json()
    assert data["status"] == "degraded"
    assert data["db"] == "unavailable"


def test_health_returns_provider(client):
    """Response includes embedding_provider for Java to log (NFR-24)."""
    with patch("api.routes.health._check_db", new_callable=AsyncMock, return_value=True):
        response = client.get("/ai/v1/health")
    assert "embedding_provider" in response.json()


def test_health_no_auth_required(client):
    """
    Warm-up must succeed without X-Internal-Key.
    (NFR-31: cold start warm-up happens before auth is available)
    """
    with patch("api.routes.health._check_db", new_callable=AsyncMock, return_value=True):
        response = client.get("/ai/v1/health")
    # Must NOT return 401
    assert response.status_code != 401
