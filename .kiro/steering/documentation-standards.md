---
inclusion: always
---
# Documentation contract
Source IDs must link requirement -> design -> code -> test (and commit where feasible). After each completed spec task update `docs/architecture/`, `docs/api/`, `docs/test-matrix.md`, `docs/traceability.csv` and README only when affected; derive endpoints from actual routes and contracts. Record deviations, uncertain design choices and tradeoffs in `docs/decisions/`. Add commands/env var NAMES only (no secrets), examples of successful/error response, diagrams Mermaid when useful. SRS originals are immutable: NEVER rewrite original DOCX or extracted reference without explicit direction. Vietnamese user-facing docs; keep English technical identifiers. No fabricated execution results.
