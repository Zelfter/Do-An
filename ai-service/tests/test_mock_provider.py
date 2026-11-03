"""
Task 01 — Python unit tests for mock embedding provider.
NFR-33: no real API calls in CI.
testing-standards.md: deterministic fixtures, no real LLM.
"""
import asyncio
import pytest
from providers.mock_provider import MockEmbeddingProvider
from core.config import settings


@pytest.fixture
def provider():
    return MockEmbeddingProvider()


def test_provider_name(provider):
    assert provider.provider_name == "mock"


def test_model_name(provider):
    assert provider.model_name == "mock-deterministic"


def test_embed_returns_correct_dimension(provider):
    """Vector shape must match settings.embedding_dim."""
    vector = asyncio.run(provider.embed("OOP là gì?"))
    assert len(vector) == settings.embedding_dim


def test_embed_is_deterministic(provider):
    """Same text must produce same vector (important for content_hash logic)."""
    v1 = asyncio.run(provider.embed("Spring Boot"))
    v2 = asyncio.run(provider.embed("Spring Boot"))
    assert v1 == v2


def test_embed_different_texts_produce_different_vectors(provider):
    """Different texts should produce different vectors."""
    v1 = asyncio.run(provider.embed("OOP"))
    v2 = asyncio.run(provider.embed("Collections"))
    assert v1 != v2


def test_embed_is_unit_vector(provider):
    """Vector should be normalised (magnitude ≈ 1.0)."""
    vector = asyncio.run(provider.embed("Java 8 Lambda"))
    magnitude = sum(f * f for f in vector) ** 0.5
    assert abs(magnitude - 1.0) < 1e-5


def test_embed_handles_empty_string(provider):
    """Empty string should not raise; returns a vector."""
    vector = asyncio.run(provider.embed(""))
    assert len(vector) == settings.embedding_dim


def test_embed_handles_vietnamese_text(provider):
    """NFR-28: Vietnamese text must not cause errors in the mock provider."""
    text = "Giải thích sự khác biệt giữa ArrayList và LinkedList trong Java."
    vector = asyncio.run(provider.embed(text))
    assert len(vector) == settings.embedding_dim
