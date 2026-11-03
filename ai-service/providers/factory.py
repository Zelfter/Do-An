"""
Provider factory — returns the configured embedding provider.
NFR-07: provider-agnostic; swap provider without touching business logic.
"""
from providers.base_provider import BaseEmbeddingProvider
from providers.mock_provider import MockEmbeddingProvider
from providers.gemini_provider import GeminiEmbeddingProvider
from core.config import settings


def get_embedding_provider() -> BaseEmbeddingProvider:
    """Return the embedding provider selected by EMBEDDING_PROVIDER env var."""
    match settings.embedding_provider:
        case "mock":
            return MockEmbeddingProvider()
        case "gemini":
            return GeminiEmbeddingProvider()
        case _:
            raise ValueError(
                f"Unknown embedding provider: {settings.embedding_provider}"
            )
