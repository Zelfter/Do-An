---
inclusion: always
---
# Proposed workspace layout
`frontend/` React/Vite UI; `backend/` Java Spring Boot, Maven (`api`, `application`, `domain`, `infrastructure` packages or existing conventions); `ai-service/` FastAPI (`api`, `schemas`, `services`, `providers`, `rag`); `docs/` source, decisions, api, architecture and traceability; `.kiro/specs/<feature>/requirements.md`, `design.md`, `tasks.md`.
Inspect existing files before generating scaffolding; respect current paths and package names. Use Flyway or equivalent versioned migrations for `app`; separate Python migrations for `rag`, with no cross-schema FK. Define explicit internal REST contracts before implementing consumers. Minimal dependencies and pool sizes for low resource usage.
