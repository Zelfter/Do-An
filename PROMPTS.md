# Prompts to paste into Kiro (one at a time)

## 1. Analyze approved SRS without coding
Read `docs/source/SRS-original.docx` if supported and full `docs/source/SRS-extracted.md`, plus `.kiro/steering/`. The new 37-page SRS is source of truth. Extract FR/NFR/BR/UC/US/AC IDs, MoSCoW and ✅/💡/⚠️. Inspect source implementation if any. Update `docs/traceability.csv` with ALL requirements, their priorities and status; update `docs/decisions/open-questions.md` with contradictions and assumptions. Summarize missing API/data decisions. DO NOT write application source or change SRS. Present proposed first feature and request my approval.

## 2. Generate first Spec (Kiro IDE Specs workflow)
Create requirements-first Spec `01-foundation-content` covering FR-01, FR-32, FR-33, FR-37 and relevant NFR/BR/AC, matching SRS §9.6 slice 1. Create `requirements.md`, `design.md`, `tasks.md`. Every acceptance criterion and task has SRS IDs. Design Java app schema and Python rag indexing API before implementation. Include migration, API contracts, validation, ACL, documentation and deterministic tests; no fake endpoints. Must before Should/Could. Stop after design to let me approve.

## 3. Implement one approved task
Implement only the next approved task in `.kiro/specs/01-foundation-content/tasks.md`. Read source IDs, steerings and existing implementation first. Create code, migrations, appropriate Java/Python/UI tests, docs and update traceability. Use mock AI and no real provider calls during CI. Run relevant test commands and report exact outcomes; do not claim success if execution failed. Show Git diff summary and stop before commit, deploy or database mutation.

## 4. Verify end-to-end Must requirements
Audit completed MVP versus all 28 Must FR, NFR Must, BR and applicable AC-01..AC-23. Identify missing tests, privacy leaks, invalid state transitions, incorrect ownership boundaries and over-scoped features. Update docs/test-matrix.md and traceability. Do not silently implement Should/Could/Won't.

## 5. Documentation sync
Compare current Git diff with approved Spec. Update docs/api, docs/architecture, README and test instructions solely for implemented behavior. Use actual controllers, Pydantic schemas, migrations, commands and results. Preserve original SRS, flag mismatches in docs/decisions. No secrets or made-up test output.
