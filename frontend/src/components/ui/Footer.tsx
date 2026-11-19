/**
 * Footer — small shared footer for both portals.
 */
interface FooterProps {
  text?: string
}

export default function Footer({ text }: FooterProps) {
  return (
    <footer className="app-footer" style={{ textAlign: 'center' }}>
      {text ?? 'Ôn Java & Phỏng vấn mô phỏng theo JD · © 2026'}
    </footer>
  )
}
