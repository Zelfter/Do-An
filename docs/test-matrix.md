# Required acceptance tests (initial)
| ID | Expected verification | Layer |
|---|---|---|
| AC-01/02 | JD validation and analysis result/limit | Java/Python integration |
| AC-03 | Confirmed topic and level used in session | Integration |
| AC-04 | Timer pauses while AI processes; UI waiting | Java + UI |
| AC-05, AC-12, BR-20 | No score/feedback during session; ended-only report | API contract/security |
| AC-06 | Time-out scores unanswered 0 and starts report | Java clock/state |
| AC-07..11 | Follow-up threshold/limit/scope, next question, no duplicate | Java domain tests |
| AC-13 | Weak topic linked to missed key points/material | Report integration |
| AC-14/15 | 429/503 retry/fallback, malformed JSON -> UNGRADED | AI mock integration |
| AC-16 | Prompt injection from JD/answers ignored as instruction | Security fixtures |
| AC-17 | Refresh resumes exact state and elapsed time | Integration/E2E |
| AC-18/19 | Admin publish completeness and indexing | Integration |
| AC-20/21 | Vietnamese and unsupported JD topics shown | Contract/E2E |
| AC-22 | Report PARTIAL retains scores when prose fails | Java integration |
| AC-23 | Cold-start waiting and retry | UI/E2E |
