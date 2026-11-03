"""
Index routes: POST/DELETE /ai/v1/index/questions and /ai/v1/index/documents.
All endpoints require X-Internal-Key header (design.md §6, core/security.py).
FR-33, FR-33-questions-index: called by Java when Admin publishes/saves content.
AC-LOG-01: IndexingService logs every call with purpose, provider, latency, status.
"""
import logging
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status

from api.schemas.index_schemas import (
    IndexDocumentRequest,
    IndexDocumentResponse,
    IndexQuestionRequest,
    IndexQuestionResponse,
)
from core.security import verify_internal_key
from services.indexing_service import IndexingService

log = logging.getLogger(__name__)

router = APIRouter(tags=["index"])


# ---- Question indexing -------------------------------------------------------


@router.post(
    "/index/questions",
    response_model=IndexQuestionResponse,
    dependencies=[Depends(verify_internal_key)],
    summary="Index a question into the RAG vector store",
)
async def index_question(request: IndexQuestionRequest) -> IndexQuestionResponse:
    """
    POST /ai/v1/index/questions
    Called by Java when Admin publishes a question (FR-33-questions-index, AC-Q-03).
    Requires X-Internal-Key header.
    """
    service = IndexingService()
    try:
        indexed, content_hash = await service.index_question(
            question_id=request.questionId,
            topic_code=request.topicCode,
            language=request.language,
            level=request.level,
            content=request.content,
            reference_answer=request.referenceAnswer,
            key_points=request.keyPoints,
        )
        return IndexQuestionResponse(indexed=indexed, contentHash=content_hash)
    except Exception as exc:
        log.error("Failed to index question %s: %s", request.questionId, exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Indexing failed: {exc}",
        )


@router.delete(
    "/index/questions/{question_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(verify_internal_key)],
    summary="Remove a question from the RAG vector store",
)
async def delete_question_index(question_id: UUID) -> None:
    """
    DELETE /ai/v1/index/questions/{question_id}
    Removes the question embedding from rag_question_vectors.
    Requires X-Internal-Key header.
    """
    service = IndexingService()
    try:
        await service.delete_question(question_id)
    except Exception as exc:
        log.error("Failed to delete question index %s: %s", question_id, exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Delete failed: {exc}",
        )


# ---- Document indexing -------------------------------------------------------


@router.post(
    "/index/documents",
    response_model=IndexDocumentResponse,
    dependencies=[Depends(verify_internal_key)],
    summary="Index a document into the RAG vector store",
)
async def index_document(request: IndexDocumentRequest) -> IndexDocumentResponse:
    """
    POST /ai/v1/index/documents
    Called by Java when Admin saves or reindexes a study document (FR-33, AC-DOC-01).
    Requires X-Internal-Key header.
    """
    service = IndexingService()
    try:
        chunk_count = await service.index_document(
            document_id=request.documentId,
            topic_code=request.topicCode,
            language=request.language,
            title=request.title,
            content_md=request.contentMd,
        )
        return IndexDocumentResponse(chunkCount=chunk_count)
    except Exception as exc:
        log.error("Failed to index document %s: %s", request.documentId, exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Indexing failed: {exc}",
        )


@router.delete(
    "/index/documents/{document_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    dependencies=[Depends(verify_internal_key)],
    summary="Remove a document from the RAG vector store",
)
async def delete_document_index(document_id: UUID) -> None:
    """
    DELETE /ai/v1/index/documents/{document_id}
    Removes all chunks for the document from rag_document_chunks.
    Requires X-Internal-Key header.
    """
    service = IndexingService()
    try:
        await service.delete_document(document_id)
    except Exception as exc:
        log.error("Failed to delete document index %s: %s", document_id, exc)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Delete failed: {exc}",
        )
