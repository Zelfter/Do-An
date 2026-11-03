"""
Embedding service — wraps the configured provider.
NFR-06, NFR-07: provider-agnostic; no torch/local model.
design.md §4.4: embedding API call abstraction.
"""
from providers.base_provider import BaseEmbeddingProvider
from providers.factory import get_embedding_provider


class EmbeddingService:
    """
    Stateless service that delegates to the configured embedding provider.
    Instantiate once per request (or share across the app — providers are stateless).
    """

    def __init__(self, provider: BaseEmbeddingProvider | None = None) -> None:
        self._provider = provider or get_embedding_provider()

    @property
    def provider_name(self) -> str:
        return self._provider.provider_name

    @property
    def model_name(self) -> str:
        return self._provider.model_name

    async def embed(self, text: str) -> list[float]:
        """Return embedding vector for text using the configured provider."""
        return await self._provider.embed(text)

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """Return embeddings for multiple texts (sequential calls)."""
        results = []
        for text in texts:
            results.append(await self._provider.embed(text))
        return results
