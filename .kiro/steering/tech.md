---
inclusion: always
---
# Tech and limits
Monorepo: React + Vite frontend (React is proposed in SRS 10.1, marked ⚠️), Spring Boot Java business backend, Python FastAPI stateless AI service, Neon PostgreSQL with schema `app` Java-owned and `rag` Python-owned, pgvector. Java 21 and Maven are implementation choices, not SRS-mandated versions. Gemini free-tier primary, Groq fallback are proposed; verify current offers and quotas before provisioning. Embeddings via API; do not load torch, sentence-transformers or local LLM.
Machine: 4 GB RAM/no GPU. No Docker Desktop or mandatory local DB; use isolated Neon dev branch and mock AI provider for automated tests. Each deployed service target <=512 MB/0.1 vCPU. Frontend static hosting, Java/Python independently deployed. Cold starts expected. Secrets exclusively env vars; never commit. Do not assume provider or hosting free-tier terms remain unchanged.
