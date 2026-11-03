-- RAG Schema migration 001: Create rag schema and enable pgvector
-- Python AI Service owns schema `rag` (SRS §10.5)
-- No cross-schema foreign keys (design.md §4.1)
-- Run by Python AI Service on startup (before first index operation)

CREATE SCHEMA IF NOT EXISTS rag;

-- pgvector extension (Neon includes it by default)
-- Installed into the first schema in search_path (default: public).
CREATE EXTENSION IF NOT EXISTS vector;

-- Keep `public` in search_path so the `vector` type (created in public by
-- the extension) resolves when migrations 002/003 declare
-- `embedding vector(768)`. `rag` stays first so unqualified CREATE TABLE
-- lands in the `rag` schema.
SET search_path TO rag, public;
