"""
Gemini embedding provider (primary) — NFR-06, NFR-07.
Uses free-tier Gemini Embedding API via httpx (no SDK to keep image small).
NFR-28: Vietnamese text support must be verified with the chosen model.
"""
import logging

import httpx

from providers.base_provider import BaseEmbeddingProvider
from core.config import settings

log = logging.getLogger(__name__)

GEMINI_EMBED_URL = (
    "https://generativelanguage.googleapis.com/v1beta/models/"
    "{model}:embedContent?key={api_key}"
)


class GeminiEmbeddingProvider(BaseEmbeddingProvider):
    """Calls Gemini Embedding API. Requires GEMINI_API_KEY env var."""

    @property
    def provider_name(self) -> str:
        return "gemini"

    @property
    def model_name(self) -> str:
        return settings.embedding_model

    async def embed(self, text: str) -> list[float]:
        url = GEMINI_EMBED_URL.format(
            model=settings.embedding_model,
            api_key=settings.gemini_api_key,
        )
        payload = {
            "model": f"models/{settings.embedding_model}",
            "content": {"parts": [{"text": text}]},
        }
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            data = resp.json()
            return data["embedding"]["values"]
