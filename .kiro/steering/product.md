---
inclusion: always
---
# Product: Java JD Interview
Source of truth: `docs/source/SRS-original.docx`; searchable extraction: `docs/source/SRS-extracted.md`. The **new 37-page SRS** supersedes the earlier Phase 4 design.
MVP: Vietnamese-language web app. Candidate pastes JD, reviews mapped topics/level, configures timed text interview with RAG-backed question pool; AI evaluates each answer and proposes follow-up; Java decides whether to ask it; after ending, report provides scores, weak topics and study materials. Admin must manage users, topics, questions and documents through UI.
Keep requirement IDs FR/NFR/BR/UC/US/AC and MoSCoW. ✅ confirmed, 💡 accepted proposal, ⚠️ assumption/estimate; never silently promote ⚠️ to a settled decision. 28 Must FRs; implement Must first. Excluded: CV (FR-03/05/08), voice (FR-36), native mobile, payments, English-language MVP, training local LLM. Should/Could only after Must or explicit instruction.
