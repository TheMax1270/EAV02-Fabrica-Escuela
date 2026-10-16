import { useState, type FormEvent } from 'react'
import { reportError, type ReportPayload, type ReportReason } from '../api/report'
import { useAuth } from '../context/useAuth'

const reasonLabels: Record<ReportReason, string> = {
  SPAM: 'Spam',
  HARASSMENT: 'Acoso',
  INAPPROPRIATE_CONTENT: 'Contenido inapropiado',
  MISINFORMATION: 'Información falsa',
  OTHER: 'Otro',
}

export default function ReportButton({ onSubmit }: { onSubmit: (payload: ReportPayload) => Promise<void> }) {
  const { isAuthenticated } = useAuth()
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState<ReportReason>('SPAM')
  const [details, setDetails] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  if (!isAuthenticated) return null

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await onSubmit({ reason, details: details.trim() || undefined })
      setDone(true)
      setOpen(false)
    } catch (failure: unknown) {
      setError(reportError(failure, 'No pudimos enviar el reporte. Intenta de nuevo.'))
    } finally {
      setBusy(false)
    }
  }

  if (done) return <p className="success-message" role="status">Reporte enviado. El equipo de moderación lo revisará.</p>

  return (
    <div className="report-control">
      <button className="text-button" type="button" onClick={() => setOpen((value) => !value)}>
        {open ? 'Cancelar reporte' : 'Reportar'}
      </button>
      {open && (
        <form className="report-form" onSubmit={submit} noValidate aria-busy={busy}>
          <div className="form-field">
            <label htmlFor="report-reason">Motivo</label>
            <select id="report-reason" value={reason} disabled={busy}
              onChange={(event) => setReason(event.target.value as ReportReason)}>
              {Object.entries(reasonLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="report-details">Detalles (opcional)</label>
            <textarea id="report-details" value={details} maxLength={500} rows={3} disabled={busy}
              onChange={(event) => setDetails(event.target.value)} />
          </div>
          {error && <p className="field-error" role="alert">{error}</p>}
          <button className="button button-danger" type="submit" disabled={busy}>
            {busy ? 'Enviando...' : 'Enviar reporte'}
          </button>
        </form>
      )}
    </div>
  )
}