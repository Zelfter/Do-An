"""
Mock embedding provider — used in tests and local dev.
Returns deterministic pseudo-random vectors without any API call.
(NFR-33: test with no real API; testing-standards.md: NEVER call real LLM in CI)
"""
import hashlib
import struct

from providers.base_provider import BaseEmbeddingProvider
from core.config import settings


class MockEmbeddingProvider(BaseEmbeddingProvider):
    """
    Produces a deterministic unit vector derived from text content SHA-256.
    Shape matches settings.embedding_dim so downstream code is unaffected.
    """

    @property
    def provider_name(self) -> str:
        return "mock"

    @property
    def model_name(self) -> str:
        return "mock-deterministic"

    async def embed(self, text: str) -> list[float]:
        """Deterministic embedding: SHA-256 → floats → normalised."""
        dim = settings.embedding_dim
        # Seed from text hash so same text → same vector
        digest = hashlib.sha256(text.encode()).digest()
        # Repeat digest to fill dim floats
        raw_bytes = (digest * ((dim * 4 // len(digest)) + 1))[: dim * 4]
        floats = list(struct.unpack(f"{dim}f", raw_bytes))
        # Normalise to unit vector
        magnitude = sum(f * f for f in floats) ** 0.5 or 1.0
        return [f / magnitude for f in floats]
