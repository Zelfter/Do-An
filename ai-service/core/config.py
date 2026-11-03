"""
Core configuration — reads from environment variables.
Never hard-code secrets here. (design.md §8, NFR-29)
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Literal


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Database — Neon PostgreSQL with pgvector (SRS §10.5)
    # psycopg3 (used directly, NOT via SQLAlchemy) expects a plain
    # "postgresql://" URI. The "+psycopg" scheme is SQLAlchemy-only and
    # would break psycopg3's connection-string parsing.
    database_url: str = "postgresql://postgres:postgres@localhost:5432/interview_dev"

    # Internal auth between Java backend and this service (design.md §6)
    internal_api_key: str = "dev-internal-key"

    # Embedding provider — NFR-06, NFR-28; no torch/local model
    # Values: "gemini" | "groq" | "mock"
    embedding_provider: Literal["gemini", "groq", "mock"] = "mock"
    embedding_model: str = "text-embedding-004"

    # Provider API keys (only needed when provider != mock)
    gemini_api_key: str = ""
    groq_api_key: str = ""

    # Embedding vector dimension — matches model output
    embedding_dim: int = 768

    # Chunking settings (design.md §4.4)
    chunk_max_tokens: int = 512
    chunk_overlap_tokens: int = 64


settings = Settings()
