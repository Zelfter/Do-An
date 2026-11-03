---
inclusion: always
---
# Testing contract
For every task: derive tests from the SRS by ID; add happy, error, boundary, security, state-transition and fallback cases. Java: JUnit 5, Mockito, Spring Boot tests; Testcontainers optional when resources allow, otherwise isolated test database and mock AI with no production data. Python: pytest, FastAPI TestClient/httpx, mock network providers. Frontend: Vitest/React Testing Library and Playwright targeted E2E. NEVER call real paid/limited LLM in default CI. Use deterministic fake responses and fixtures. Document commands and actually execute available tests; never claim passing tests without observed output. Maintain `docs/traceability.csv` and `docs/test-matrix.md`.
Must cover AC-01..AC-23 subject to explicit Should dependencies; especially score leakage BR-20, idempotent answers, >=70 follow-up with <2 limit and JD scope, question deduplication, prompt injection, timeout/warmup, retry/fallback/UNGRADED, report PARTIAL, ACL owner/admin, resume after refresh, index publish validation. Quality metrics should not substitute for business assertions.
