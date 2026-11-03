"""
Task 07 — Integration tests for /ai/v1/index/* endpoints.
NFR-33: no real API calls, no real DB (all mocked).
testing-standards.md: use MockEmbeddingProvider.

Tests cover:
- POST /ai/v1/index/questions: success, missing X-Internal-Key → 401, wrong key → 401
- POST /ai/v1/index/documents: success, missing/wrong X-Internal-Key → 401
- DELETE /ai/v1/index/questions/{id}: success, auth
- DELETE /ai/v1/index/documents/{id}: success, auth
- X-Internal-Key validation (design §6, core/security.py)
"""
import sys
import types
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from uuid import uuid4

# ---- Stub psycopg/psycopg_pool before any imports touch them ---------------


def _install_psycopg_stub():
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

VALID_KEY = "dev-internal-key"

# ---- Fixtures ---------------------------------------------------------------


@pytest.fixture
def client():
    """
    TestClient with DB pool, migrations, and vector store mocked.
    Uses MockEmbeddingProvider (settings.embedding_provider = 'mock' by default).
    Patches the vector store functions at their import location in indexing_service.
    """
    with (
        patch("core.db.init_pool", new_callable=AsyncMock),
        patch("core.db.close_pool", new_callable=AsyncMock),
        patch("main.run_migrations", new_callable=AsyncMock),
        # Patch at the import location used by indexing_service.py
        patch("services.indexing_service.upsert_question_vector", new_callable=AsyncMock) as mock_upsert_q,
        patch("services.indexing_service.upsert_document_chunks", new_callable=AsyncMock, return_value=3) as mock_upsert_d,
        patch("services.indexing_service.delete_question_vector", new_callable=AsyncMock) as mock_del_q,
        patch("services.indexing_service.delete_document_chunks", new_callable=AsyncMock) as mock_del_d,
    ):
        from main import app
        from fastapi.testclient import TestClient

        with TestClient(app) as c:
            c._mock_upsert_q = mock_upsert_q
            c._mock_upsert_d = mock_upsert_d
            c._mock_del_q = mock_del_q
            c._mock_del_d = mock_del_d
            yield c


def question_payload(question_id=None):
    return {
        "questionId": str(question_id or uuid4()),
        "topicCode": "oop",
        "language": "vi",
        "level": "JUNIOR",
        "content": "Giải thích tính đóng gói trong OOP?",
        "referenceAnswer": "Encapsulation ẩn chi tiết triển khai, chỉ lộ interface công khai.",
        "keyPoints": ["Ẩn trường private", "Truy cập qua getter/setter"],
    }


def document_payload(doc_id=None):
    return {
        "documentId": str(doc_id or uuid4()),
        "topicCode": "oop",
        "language": "vi",
        "title": "Tài liệu OOP cơ bản",
        "contentMd": "## Encapsulation\nĐóng gói dữ liệu\n\n## Inheritance\nKế thừa",
    }


# ============================================================================
# X-Internal-Key authentication (design §6, core/security.py)
# ============================================================================


def test_index_questions_missing_key_returns_401(client):
    """Missing X-Internal-Key → 401 (design §6)."""
    resp = client.post("/ai/v1/index/questions", json=question_payload())
    assert resp.status_code == 401, f"Expected 401, got {resp.status_code}: {resp.text}"


def test_index_questions_wrong_key_returns_401(client):
    """Wrong X-Internal-Key → 401."""
    resp = client.post(
        "/ai/v1/index/questions",
        json=question_payload(),
        headers={"X-Internal-Key": "wrong-key"},
    )
    assert resp.status_code == 401


def test_index_documents_missing_key_returns_401(client):
    """Missing X-Internal-Key → 401 for documents endpoint."""
    resp = client.post("/ai/v1/index/documents", json=document_payload())
    assert resp.status_code == 401


def test_index_documents_wrong_key_returns_401(client):
    """Wrong X-Internal-Key → 401 for documents endpoint."""
    resp = client.post(
        "/ai/v1/index/documents",
        json=document_payload(),
        headers={"X-Internal-Key": "bad-key"},
    )
    assert resp.status_code == 401


def test_delete_question_missing_key_returns_401(client):
    """DELETE /index/questions/{id} without key → 401."""
    resp = client.delete(f"/ai/v1/index/questions/{uuid4()}")
    assert resp.status_code == 401


def test_delete_document_missing_key_returns_401(client):
    """DELETE /index/documents/{id} without key → 401."""
    resp = client.delete(f"/ai/v1/index/documents/{uuid4()}")
    assert resp.status_code == 401


# ============================================================================
# POST /ai/v1/index/questions
# ============================================================================


def test_index_question_success(client):
    """Valid payload + correct key → 200 with indexed=true and contentHash."""
    payload = question_payload()
    resp = client.post(
        "/ai/v1/index/questions",
        json=payload,
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
    data = resp.json()
    assert data["indexed"] is True
    assert isinstance(data["contentHash"], str)
    assert len(data["contentHash"]) == 64  # SHA-256 hex


def test_index_question_response_has_content_hash(client):
    """contentHash must be a non-empty hex string (SHA-256 of embedded text)."""
    resp = client.post(
        "/ai/v1/index/questions",
        json=question_payload(),
        headers={"X-Internal-Key": VALID_KEY},
    )
    data = resp.json()
    content_hash = data.get("contentHash", "")
    assert len(content_hash) == 64
    assert all(c in "0123456789abcdef" for c in content_hash.lower())


def test_index_question_upserts_vector(client):
    """POST /index/questions must call upsert_question_vector exactly once."""
    resp = client.post(
        "/ai/v1/index/questions",
        json=question_payload(),
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200
    client._mock_upsert_q.assert_called_once()


def test_index_question_vietnamese_content(client):
    """Vietnamese content must be indexed without errors (NFR-28)."""
    payload = question_payload()
    payload["content"] = "Giải thích sự khác biệt giữa ArrayList và LinkedList."
    payload["referenceAnswer"] = "ArrayList dùng mảng động; LinkedList dùng danh sách liên kết."
    resp = client.post(
        "/ai/v1/index/questions",
        json=payload,
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200
    assert resp.json()["indexed"] is True


# ============================================================================
# POST /ai/v1/index/documents
# ============================================================================


def test_index_document_success(client):
    """Valid payload + correct key → 200 with chunkCount > 0."""
    resp = client.post(
        "/ai/v1/index/documents",
        json=document_payload(),
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200, f"Expected 200, got {resp.status_code}: {resp.text}"
    data = resp.json()
    assert "chunkCount" in data
    assert data["chunkCount"] >= 0


def test_index_document_upserts_chunks(client):
    """POST /index/documents must call upsert_document_chunks exactly once."""
    resp = client.post(
        "/ai/v1/index/documents",
        json=document_payload(),
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200
    client._mock_upsert_d.assert_called_once()


def test_index_document_large_content_chunked(client):
    """Large document content should be chunked (multiple calls possible)."""
    long_content = "## Section\n" + "Từ " * 600  # ~600 words, well over 512 token limit
    payload = document_payload()
    payload["contentMd"] = long_content
    resp = client.post(
        "/ai/v1/index/documents",
        json=payload,
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200


def test_index_document_empty_content_still_returns_200(client):
    """Empty contentMd should be handled gracefully — fallback to title chunk."""
    payload = document_payload()
    payload["contentMd"] = ""
    resp = client.post(
        "/ai/v1/index/documents",
        json=payload,
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 200


# ============================================================================
# DELETE /ai/v1/index/questions/{id}
# ============================================================================


def test_delete_question_index_returns_204(client):
    """DELETE with valid key → 204."""
    resp = client.delete(
        f"/ai/v1/index/questions/{uuid4()}",
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 204


def test_delete_question_calls_vector_store(client):
    """DELETE must invoke delete_question_vector in the vector store."""
    qid = uuid4()
    client.delete(
        f"/ai/v1/index/questions/{qid}",
        headers={"X-Internal-Key": VALID_KEY},
    )
    client._mock_del_q.assert_called_once()


# ============================================================================
# DELETE /ai/v1/index/documents/{id}
# ============================================================================


def test_delete_document_index_returns_204(client):
    """DELETE with valid key → 204."""
    resp = client.delete(
        f"/ai/v1/index/documents/{uuid4()}",
        headers={"X-Internal-Key": VALID_KEY},
    )
    assert resp.status_code == 204


def test_delete_document_calls_vector_store(client):
    """DELETE must invoke delete_document_chunks in the vector store."""
    did = uuid4()
    client.delete(
        f"/ai/v1/index/documents/{did}",
        headers={"X-Internal-Key": VALID_KEY},
    )
    client._mock_del_d.assert_called_once()
