/**
 * Shared inline styles for candidate-facing pages (NFR-22: simple, functional UI).
 *
 * The project uses inline React.CSSProperties (no Tailwind/Bootstrap). This is
 * the candidate counterpart to adminStyles.ts: a lighter, friendlier theme used
 * by the Dashboard, Interview Room and Report pages.
 */
import type { CSSProperties } from 'react'

export const interviewStyles: Record<string, CSSProperties> = {
  // ── Layout ────────────────────────────────────────────────────────────────
  page: { minHeight: '100vh', background: '#f5f7fb', display: 'flex', flexDirection: 'column' },
  pageNarrow: { maxWidth: 960, margin: '0 auto', padding: '1.5rem', width: '100%', boxSizing: 'border-box' as const },

  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 1.5rem',
    height: 56,
    background: '#4f46e5',
    color: '#fff',
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
  },
  brand: { fontWeight: 700, fontSize: '1rem', letterSpacing: 0.2 },
  headerNav: { display: 'flex', alignItems: 'center', gap: '1rem' },
  userInfo: { fontSize: '0.85rem', opacity: 0.9 },
  ghostButtonLight: {
    background: 'transparent',
    border: '1px solid rgba(255,255,255,0.5)',
    color: '#fff',
    padding: '0.25rem 0.75rem',
    borderRadius: 6,
    cursor: 'pointer',
    fontSize: '0.85rem',
  },

  nav: { display: 'flex', gap: '0.25rem', padding: '0 1.5rem', background: '#fff', borderBottom: '1px solid #ececf3' },
  navLink: { padding: '0.7rem 0.9rem', color: '#6b7280', textDecoration: 'none', fontSize: '0.9rem', borderBottom: '2px solid transparent' },
  navLinkActive: { color: '#4f46e5', fontWeight: 600, borderBottom: '2px solid #4f46e5' },

  main: { flex: 1, padding: '1.5rem', maxWidth: 960, margin: '0 auto', width: '100%', boxSizing: 'border-box' as const },

  // ── Cards / sections ──────────────────────────────────────────────────────
  card: { background: '#fff', borderRadius: 12, padding: '1.5rem', boxShadow: '0 1px 3px rgba(0,0,0,0.06)', marginBottom: '1rem' },
  sectionTitle: { margin: '0 0 0.75rem', fontSize: '1.1rem', color: '#1f2937' },
  mutedText: { color: '#6b7280', fontSize: '0.875rem' },

  // ── Buttons ────────────────────────────────────────────────────────────────
  primaryButton: { background: '#4f46e5', color: '#fff', border: 'none', borderRadius: 8, padding: '0.6rem 1.1rem', cursor: 'pointer', fontSize: '0.95rem', fontWeight: 600 },
  secondaryButton: { background: '#fff', color: '#374151', border: '1px solid #d1d5db', borderRadius: 8, padding: '0.6rem 1.1rem', cursor: 'pointer', fontSize: '0.95rem' },
  dangerButton: { background: '#ef4444', color: '#fff', border: 'none', borderRadius: 8, padding: '0.6rem 1.1rem', cursor: 'pointer', fontSize: '0.95rem', fontWeight: 600 },
  successButton: { background: '#16a34a', color: '#fff', border: 'none', borderRadius: 8, padding: '0.6rem 1.1rem', cursor: 'pointer', fontSize: '0.95rem', fontWeight: 600 },
  disabledButton: { opacity: 0.6, cursor: 'not-allowed' },

  // ── Forms ──────────────────────────────────────────────────────────────────
  field: { marginBottom: '0.85rem' },
  label: { display: 'block', marginBottom: 6, fontWeight: 600, fontSize: '0.9rem', color: '#374151' },
  input: { width: '100%', padding: '0.55rem 0.75rem', border: '1px solid #d1d5db', borderRadius: 8, fontSize: '0.95rem', boxSizing: 'border-box' as const, fontFamily: 'inherit' },
  textarea: { width: '100%', padding: '0.6rem 0.8rem', border: '1px solid #d1d5db', borderRadius: 8, fontSize: '0.95rem', boxSizing: 'border-box' as const, minHeight: 120, fontFamily: 'inherit', resize: 'vertical' as const, lineHeight: 1.5 },
  select: { width: '100%', padding: '0.5rem 0.5rem', border: '1px solid #d1d5db', borderRadius: 8, fontSize: '0.95rem', boxSizing: 'border-box' as const },

  // ── Feedback ───────────────────────────────────────────────────────────────
  errorText: { color: '#dc2626', fontSize: '0.9rem', margin: '0.5rem 0' },
  successText: { color: '#16a34a', fontSize: '0.9rem', margin: '0.5rem 0' },
  infoText: { color: '#2563eb', fontSize: '0.9rem', margin: '0.5rem 0' },

  // ── Badges / chips ──────────────────────────────────────────────────────────
  badge: { display: 'inline-block', padding: '0.1rem 0.55rem', borderRadius: 999, fontSize: '0.78rem', fontWeight: 600 },
  badgeInfo: { background: '#eef2ff', color: '#4338ca', border: '1px solid #c7d2fe' },
  badgeWarn: { background: '#fffbeb', color: '#b45309', border: '1px solid #fde68a' },
  badgeDanger: { background: '#fef2f2', color: '#b91c1c', border: '1px solid #fecaca' },
  badgeSuccess: { background: '#f0fdf4', color: '#15803d', border: '1px solid #bbf7d0' },
  chipList: { display: 'flex', flexWrap: 'wrap', gap: '0.4rem' },
  chip: { background: '#eef2ff', color: '#4338ca', border: '1px solid #c7d2fe', borderRadius: 999, padding: '0.2rem 0.7rem', fontSize: '0.8rem' },

  // ── Table (report) ──────────────────────────────────────────────────────────
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  th: { textAlign: 'left', padding: '0.6rem 0.75rem', borderBottom: '2px solid #ececf3', fontWeight: 600, whiteSpace: 'nowrap' },
  td: { padding: '0.6rem 0.75rem', borderBottom: '1px solid #ececf3', verticalAlign: 'top' },
}
