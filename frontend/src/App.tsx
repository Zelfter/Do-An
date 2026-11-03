/**
 * Root component — provides AuthContext and renders the router.
 * Architecture rule: this component NEVER imports any AI service or LLM client.
 * (architecture-guardrails.md)
 */
import { AuthProvider } from './contexts/AuthContext'
import AppRouter from './router/AppRouter'

function App() {
  return (
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  )
}

export default App
