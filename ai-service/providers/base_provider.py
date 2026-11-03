"""
Abstract base for embedding providers.
Abstracts over Gemini, Groq, and Mock to satisfy NFR-07 (provider-agnostic).
"""
from abc import ABC, abstractmethod


class BaseEmbeddingProvider(ABC):
    """
    All embedding providers implement this interface.
    Java Backend never calls this — Python AI Service only. (architecture-guardrails.md)
    """

    @abstractmethod
    async def embed(self, text: str) -> list[float]:
        """Return embedding vector for the given text."""
        ...

    @property
    @abstractmethod
    def provider_name(self) -> str:
        """Human-readable provider name for logging (NFR-24)."""
        ...

    @property
    @abstractmethod
    def model_name(self) -> str:
        """Model identifier for logging (NFR-24)."""
        ...
