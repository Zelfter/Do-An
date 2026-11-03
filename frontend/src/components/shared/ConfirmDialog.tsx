/**
 * ConfirmDialog — generic confirmation modal (NFR-26: Vietnamese).
 * Used for delete confirmations across admin pages.
 */
import { adminStyles } from './adminStyles'

interface ConfirmDialogProps {
  /** Title shown at the top of the modal */
  title: string
  /** Question / instruction text */
  message: string
  /** Label for the confirm (danger) button */
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
}

export default function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel }: ConfirmDialogProps) {
  return (
    <div style={adminStyles.modalOverlay} role="dialog" aria-modal="true" aria-label={title}>
      <div style={adminStyles.modalCard}>
        <h2 style={adminStyles.modalTitle}>{title}</h2>
        <p style={{ marginBottom: '1rem' }}>{message}</p>
        <div style={adminStyles.modalActions}>
          <button type="button" style={adminStyles.secondaryButton} onClick={onCancel}>
            Hủy
          </button>
          <button type="button" style={adminStyles.dangerButton} onClick={onConfirm} autoFocus>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
