"""
Pydantic request/response models for /ai/v1/index/* endpoints.
FR-33, FR-33-questions-index: indexing payloads from Java backend.
design.md §4.2: request/response contracts.
"""
from pydantic import BaseModel, Field
from typing import Optional
from uuid import UUID


# ---- Question indexing ----

class IndexQuestionRequest(BaseModel):
    """POST /ai/v1/index/questions — payload from Java when Admin publishes a question."""
    questionId: UUID
    topicCode: str
    language: str = "vi"
    level: str  # JUNIOR | MID | SENIOR
    content: str
    referenceAnswer: str
    keyPoints: list[str] = Field(default_factory=list)


class IndexQuestionResponse(BaseModel):
    """Response for POST /ai/v1/index/questions."""
    indexed: bool
    contentHash: str


# ---- Document indexing ----

class IndexDocumentRequest(BaseModel):
    """POST /ai/v1/index/documents — payload from Java when Admin saves/reindexes a document."""
    documentId: UUID
    topicCode: str
    language: str = "vi"
    title: str
    contentMd: str


class IndexDocumentResponse(BaseModel):
    """Response for POST /ai/v1/index/documents."""
    chunkCount: int
