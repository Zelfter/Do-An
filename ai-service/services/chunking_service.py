"""
Markdown-aware text chunker for RAG indexing.
design.md §4.4: chunk on H2/H3 headings; max 512 tokens; overlap 64 tokens.
NFR-06: no heavy ML libraries — plain string operations only.
"""
import re
from core.config import settings


def _estimate_tokens(text: str) -> int:
    """
    Rough token estimate: split on whitespace.
    1 word ≈ 1.3 tokens for English/Vietnamese; we use 1 word = 1 token for simplicity
    since the chunker just needs to be approximately correct for splitting.
    """
    return len(text.split())


def chunk_markdown(text: str,
                   max_tokens: int | None = None,
                   overlap_tokens: int | None = None) -> list[str]:
    """
    Split a Markdown document into chunks.

    Strategy (design.md §4.4):
    1. Split on H2/H3 headings (## / ###) as natural section boundaries.
    2. Each section is sub-split if it exceeds max_tokens.
    3. Adjacent chunks share overlap_tokens words for context continuity.

    Returns a list of non-empty chunk strings.
    """
    max_t = max_tokens if max_tokens is not None else settings.chunk_max_tokens
    overlap_t = overlap_tokens if overlap_tokens is not None else settings.chunk_overlap_tokens

    if not text or not text.strip():
        return []

    # Split on H2 and H3 headings, keeping the heading with its section.
    heading_pattern = re.compile(r"(?=^#{2,3}\s)", re.MULTILINE)
    sections = heading_pattern.split(text)

    chunks: list[str] = []
    for section in sections:
        section = section.strip()
        if not section:
            continue

        words = section.split()
        if len(words) <= max_t:
            chunks.append(section)
        else:
            # Sub-split large sections with sliding window.
            start = 0
            while start < len(words):
                end = min(start + max_t, len(words))
                chunk_words = words[start:end]
                chunks.append(" ".join(chunk_words))
                if end == len(words):
                    break
                # Advance with overlap.
                start = end - overlap_t

    return [c for c in chunks if c.strip()]


def chunk_question(content: str, reference_answer: str) -> list[str]:
    """
    For questions, embed content + reference_answer as a single unit (design.md §4.4).
    Returns a single-element list.
    """
    combined = f"{content}\n{reference_answer}"
    return [combined.strip()] if combined.strip() else []
