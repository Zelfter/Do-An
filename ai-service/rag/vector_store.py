"""
pgvector upsert operations for schema `rag`.
design.md §4.1: rag_question_vectors and rag_document_chunks tables.
SRS §10.5: Python owns schema `rag`; no cross-schema FK.
NFR-06: no heavy ML dependencies — pure SQL via psycopg3.
"""
import hashlib
import logging
from datetime import datetime, timezone
from uuid import UUID

from core.db import get_conn

log = logging.getLogger(__name__)


def _content_hash(text: str) -> str:
    """SHA-256 hash of text content to detect changes (design §4.1)."""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


async def upsert_question_vector(
    question_id: UUID,
    embedding: list[float],
    content_text: str,
) -> None:
    """
    Upsert a question embedding into rag_question_vectors.
    design.md §4.1: question_id is the PK; content_hash tracks changes.
    """
    content_hash = _content_hash(content_text)
    now = datetime.now(timezone.utc)

    async with get_conn() as conn:
        await conn.execute(
            """
            INSERT INTO rag.rag_question_vectors (question_id, embedding, content_hash, indexed_at)
            VALUES (%s, %s::vector, %s, %s)
            ON CONFLICT (question_id) DO UPDATE
              SET embedding    = EXCLUDED.embedding,
                  content_hash = EXCLUDED.content_hash,
                  indexed_at   = EXCLUDED.indexed_at
            """,
            (str(question_id), embedding, content_hash, now),
        )
    log.info("Upserted question vector: questionId=%s contentHash=%s", question_id, content_hash[:8])


async def upsert_document_chunks(
    document_id: UUID,
    chunks: list[str],
    embeddings: list[list[float]],
) -> int:
    """
    Delete existing chunks for document_id, then insert new ones.
    design.md §4.1: UNIQUE(document_id, chunk_index).

    Returns the number of chunks inserted.
    """
    if len(chunks) != len(embeddings):
        raise ValueError("chunks and embeddings must have the same length")

    now = datetime.now(timezone.utc)
    doc_id_str = str(document_id)

    async with get_conn() as conn:
        # Delete old chunks first (full re-index).
        await conn.execute(
            "DELETE FROM rag.rag_document_chunks WHERE document_id = %s",
            (doc_id_str,),
        )

        # Insert new chunks.
        for chunk_index, (chunk_text, embedding) in enumerate(zip(chunks, embeddings)):
            chunk_id = str(__import__("uuid").uuid4())
            await conn.execute(
                """
                INSERT INTO rag.rag_document_chunks
                  (id, document_id, chunk_index, chunk_text, embedding, indexed_at)
                VALUES (%s, %s, %s, %s, %s::vector, %s)
                """,
                (chunk_id, doc_id_str, chunk_index, chunk_text, embedding, now),
            )

    log.info("Upserted %d chunks for documentId=%s", len(chunks), document_id)
    return len(chunks)


async def delete_question_vector(question_id: UUID) -> None:
    """Remove a question embedding from rag_question_vectors."""
    async with get_conn() as conn:
        await conn.execute(
            "DELETE FROM rag.rag_question_vectors WHERE question_id = %s",
            (str(question_id),),
        )
    log.info("Deleted question vector: questionId=%s", question_id)


async def delete_document_chunks(document_id: UUID) -> None:
    """Remove all chunks for a document from rag_document_chunks."""
    async with get_conn() as conn:
        await conn.execute(
            "DELETE FROM rag.rag_document_chunks WHERE document_id = %s",
            (str(document_id),),
        )
    log.info("Deleted chunks for documentId=%s", document_id)
