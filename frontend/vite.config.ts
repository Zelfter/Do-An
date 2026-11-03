import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Vite config — React/Vite frontend (D-01: React confirmed, steering/tech.md)
 *
 * Dev proxy: frontend at :5173 proxies /api/v1 → Java backend :8080
 * Architecture guardrail: frontend NEVER calls Python service or LLM directly.
 * (architecture-guardrails.md)
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    // Vitest config (testing-standards.md: Vitest + React Testing Library)
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
  build: {
    // Output for static hosting (Vercel) — SRS §10.6
    outDir: 'dist',
    sourcemap: false,
  },
})
