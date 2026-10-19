import { useEffect, useState, type FormEvent } from 'react'
import { reportError, type ReportPayload, type ReportReason } from '../api/report'
import { useAuth } from '../context/useAuth'

const reasonLabels: Record<ReportReason, string> = {
  SPAM: 'Spam',
  HARASSMENT: 'Acoso',
  INAPPROPRIATE_CONTENT: 'Contenido inapropiado',
  MISINFORMATION: 'Información falsa',
  OTHER: 'Otro',
}

export default function ReportButton({ onSubmit, statusCheck }: {
  onSubmit: (payload: ReportPayload) => Promise<void>
  statusCheck?: () => Promise<boolean>
}) {
  const { isAuthenticated } = useAuth()
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState<ReportReason>('SPAM')
  const [details, setDetails] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [showToast, setShowToast] = useState(false)

  useEffect(() => {
    let cancelled = false
    if (statusCheck && isAuthenticated) {
      statusCheck().then((reported) => { if (!cancelled) setDone(reported) }).catch(() => undefined)
    }
    return () => { cancelled = true }
  }, [isAuthenticated])

  useEffect(() => {
    if (!showToast) return undefined
    const timeout = window.setTimeout(() => setShowToast(false), 6500)
    return () => window.clearTimeout(timeout)
  }, [showToast])

  if (!isAuthenticated) return null

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await onSubmit({ reason, details: details.trim() || undefined })
      setDone(true)
      setShowToast(true)
      setOpen(false)
    } catch (failure: unknown) {
      setError(reportError(failure, 'No pudimos enviar el reporte. Intenta de nuevo.'))
    } finally {
      setBusy(false)
    }
  }

  if (done) return <>
    <span className="reported-badge" role="status">Reportado · en revisión</span>
    {showToast && <div className="report-toast report-toast-floating" role="status">Agradecemos tu ayuda para mantener la comunidad segura. Si encontramos que este contenido viola nuestras reglas, tomaremos las medidas correspondientes. No le notificaremos a la otra persona quién hizo el reporte.</div>}
  </>

  return (
    <div className="report-control">
      <button className="report-trigger" type="button" onClick={() => setOpen(true)}>
        <span aria-hidden="true">⚑</span> Reportar
      </button>
      {open && (
        <div className="modal-overlay" role="presentation">
          <div className="report-dialog" role="dialog" aria-modal="true" aria-labelledby="report-title">
            <h2 id="report-title">Reportar contenido</h2>
            <p>El equipo de moderación revisará tu reporte. Usa esta opción solo cuando exista un posible incumplimiento de las normas.</p>
            <form className="report-form" onSubmit={submit} noValidate aria-busy={busy}>
              <div className="form-field">
                <label htmlFor="report-reason">Motivo</label>
                <select id="report-reason" value={reason} disabled={busy}
                  onChange={(event) => setReason(event.target.value as ReportReason)}>
                  {Object.entries(reasonLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="report-details">Detalles (opcional)</label>
                <textarea id="report-details" value={details} maxLength={500} rows={3} disabled={busy}
                  onChange={(event) => setDetails(event.target.value)} />
              </div>
              {error && <p className="field-error" role="alert">{error}</p>}
              <div className="dialog-actions">
                <button className="button button-secondary" type="button" disabled={busy} onClick={() => setOpen(false)}>Cancelar</button>
                <button className="button button-danger" type="submit" disabled={busy}>{busy ? 'Enviando...' : 'Enviar reporte'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}