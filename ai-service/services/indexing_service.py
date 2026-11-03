"""
Indexing orchestration service.
Coordinates: chunk → embed → upsert for questions and documents.
design.md §4.3, §4.4, §4.5.
NFR-24: logs each call with purpose, provider, latency, status (structured JSON).
"""
import logging
import time
from uuid import UUID

from services.chunking_service import chunk_markdown, chunk_question
from services.embedding_service import EmbeddingService
from rag.vector_store import (
    upsert_question_vector,
    upsert_document_chunks,
    delete_question_vector,
    delete_document_chunks,
)
from providers.base_provider import BaseEmbeddingProvider

log = logging.getLogger(__name__)


class IndexingService:
    """
    Orchestrates the full index pipeline: chunk → embed → upsert.
    Stateless; one instance per request is fine.
    """

    def __init__(self, provider: BaseEmbeddingProvider | None = None) -> None:
        self._embedding_service = EmbeddingService(provider)

    @property
    def provider_name(self) -> str:
        return self._embedding_service.provider_name

    @property
    def model_name(self) -> str:
        return self._embedding_service.model_name

    async def index_question(
        self,
        question_id: UUID,
        topic_code: str,
        language: str,
        level: str,
        content: str,
        reference_answer: str,
        key_points: list[str],
    ) -> tuple[bool, str]:
        """
        Index a question into rag_question_vectors.

        design.md §4.4: embed content + reference_answer as a single unit.
        Returns (indexed: bool, content_hash: str).
        NFR-24: logs result with latency.
        """
        start = time.monotonic()
        try:
            chunks = chunk_question(content, reference_answer)
            if not chunks:
                raise ValueError("Question content is empty after chunking")

            embeddings = await self._embedding_service.embed_batch(chunks)
            # Single unit: use the first (and only) embedding.
            embedding = embeddings[0]
            content_text = chunks[0]

            await upsert_question_vector(question_id, embedding, content_text)

            latency_ms = int((time.monotonic() - start) * 1000)
            log.info(
                '{"purpose":"INDEX_QUESTION","questionId":"%s","topicCode":"%s","provider":"%s","model":"%s",'
                '"status":"SUCCESS","latency_ms":%d}',
                question_id, topic_code, self.provider_name, self.model_name, latency_ms,
            )
            import hashlib
            content_hash = hashlib.sha256(content_text.encode()).hexdigest()
            return True, content_hash

        except Exception as exc:
            latency_ms = int((time.monotonic() - start) * 1000)
            log.error(
                '{"purpose":"INDEX_QUESTION","questionId":"%s","provider":"%s","status":"FAILED",'
                '"latency_ms":%d,"error":"%s"}',
                question_id, self.provider_name, latency_ms, str(exc),
            )
            raise

    async def index_document(
        self,
        document_id: UUID,
        topic_code: str,
        language: str,
        title: str,
        content_md: str,
    ) -> int:
        """
        Index a document into rag_document_chunks.

        design.md §4.4: chunk on H2/H3 headings; max 512 tokens; overlap 64.
        Returns chunk_count.
        NFR-24: logs result with latency.
        """
        start = time.monotonic()
        try:
            full_text = f"# {title}\n\n{content_md}" if content_md else f"# {title}"
            chunks = chunk_markdown(full_text)
            if not chunks:
                chunks = [f"# {title}"]  # Fallback: title-only chunk

            embeddings = await self._embedding_service.embed_batch(chunks)
            chunk_count = await upsert_document_chunks(document_id, chunks, embeddings)

            latency_ms = int((time.monotonic() - start) * 1000)
            log.info(
                '{"purpose":"INDEX_DOCUMENT","documentId":"%s","topicCode":"%s","provider":"%s","model":"%s",'
                '"status":"SUCCESS","chunkCount":%d,"latency_ms":%d}',
                document_id, topic_code, self.provider_name, self.model_name, chunk_count, latency_ms,
            )
            return chunk_count

        except Exception as exc:
            latency_ms = int((time.monotonic() - start) * 1000)
            log.error(
                '{"purpose":"INDEX_DOCUMENT","documentId":"%s","provider":"%s","status":"FAILED",'
                '"latency_ms":%d,"error":"%s"}',
                document_id, self.provider_name, latency_ms, str(exc),
            )
            raise

    async def delete_question(self, question_id: UUID) -> None:
        """Remove a question's vector from the RAG store."""
        await delete_question_vector(question_id)

    async def delete_document(self, document_id: UUID) -> None:
        """Remove all chunks for a document from the RAG store."""
        await delete_document_chunks(document_id)
