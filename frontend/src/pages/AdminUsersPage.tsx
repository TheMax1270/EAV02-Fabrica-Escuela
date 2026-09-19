import { useCallback, useEffect, useState } from 'react'
import axios from 'axios'
import { listUsers, reactivateUser, suspendUser, type AccountStatus, type AdminUser, type UserPage } from '../api/admin'
import type { ApiErrorResponse, AuthUser } from '../api/auth'
import SiteHeader from '../components/SiteHeader'
import { useAuth } from '../context/useAuth'

const PAGE_SIZE = 20

const roleLabels: Record<AuthUser['role'], string> = {
  DEVELOPER: 'Desarrollador',
  ADMIN: 'Administrador',
}

const statusLabels: Record<AccountStatus, string> = {
  ACTIVE: 'Activa',
  SUSPENDED: 'Suspendida',
}

type StatusFilter = AccountStatus | 'ALL'

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' })
}

function errorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiErrorResponse>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return fallback
}

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [data, setData] = useState<UserPage | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [pendingId, setPendingId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)

  const retry = useCallback(() => {
    setIsLoading(true)
    setReload((current) => current + 1)
  }, [])

  useEffect(() => {
    let cancelled = false
    listUsers({ page, size: PAGE_SIZE, status: statusFilter === 'ALL' ? undefined : statusFilter })
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch(() => {
        if (!cancelled) setError('No pudimos cargar los usuarios. Intenta de nuevo.')
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => { cancelled = true }
  }, [page, statusFilter, reload])

  function changeFilter(value: StatusFilter) {
    setIsLoading(true)
    setStatusFilter(value)
    setPage(0)
    setActionError(null)
    setActionMessage(null)
  }

  function goToPage(next: number) {
    setIsLoading(true)
    setPage(next)
  }

  function replaceUser(updated: AdminUser) {
    setData((current) => current
      ? { ...current, content: current.content.map((row) => (row.id === updated.id ? updated : row)) }
      : current)
  }

  async function changeStatus(target: AdminUser) {
    if (pendingId) return
    const suspending = target.status === 'ACTIVE'
    if (suspending && !window.confirm(`¿Suspender la cuenta de ${target.username}? No podrá iniciar sesión hasta que la reactives.`)) {
      return
    }
    setPendingId(target.id)
    setActionError(null)
    setActionMessage(null)
    try {
      const updated = suspending ? await suspendUser(target.id) : await reactivateUser(target.id)
      replaceUser(updated)
      setActionMessage(suspending
        ? `La cuenta de ${updated.username} fue suspendida.`
        : `La cuenta de ${updated.username} fue reactivada.`)
    } catch (requestError) {
      setActionError(errorMessage(requestError, 'No pudimos actualizar la cuenta. Intenta de nuevo.'))
      // A 409 means the table is stale (someone else changed the account): refresh it.
      if (axios.isAxiosError(requestError) && requestError.response?.status === 409) retry()
    } finally {
      setPendingId(null)
    }
  }

  const totalPages = data?.totalPages ?? 0
  const hasRows = Boolean(data && data.content.length > 0)

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width">
        <div className="page-heading">
          <p className="eyebrow">ADMINISTRACIÓN</p>
          <h1>Gestión de usuarios</h1>
          <p>Consulta las cuentas registradas y controla su acceso a la plataforma.</p>
        </div>

        <div className="admin-toolbar">
          <div className="form-field admin-filter">
            <label htmlFor="status-filter">Estado</label>
            <select id="status-filter" value={statusFilter} disabled={isLoading}
              onChange={(event) => changeFilter(event.target.value as StatusFilter)}>
              <option value="ALL">Todas</option>
              <option value="ACTIVE">Activas</option>
              <option value="SUSPENDED">Suspendidas</option>
            </select>
          </div>
          {data && (
            <p className="admin-count" role="status">
              {data.totalElements} {data.totalElements === 1 ? 'cuenta' : 'cuentas'}
            </p>
          )}
        </div>

        {actionMessage && <p className="success-message" role="status">{actionMessage}</p>}
        {actionError && <p className="form-error" role="alert">{actionError}</p>}
        {isLoading && <p className="loading-note" role="status">Cargando usuarios...</p>}
        {error && (
          <div className="form-error" role="alert">
            <span>{error}</span>
            <button className="text-button" type="button" onClick={retry}>Reintentar</button>
          </div>
        )}

        {!error && data && (
          <>
            <div className="admin-table-wrap">
              <table className="admin-table" aria-busy={isLoading}>
                <thead>
                  <tr>
                    <th scope="col">Nombre</th>
                    <th scope="col">Usuario</th>
                    <th scope="col">Correo</th>
                    <th scope="col">Rol</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Registro</th>
                    <th scope="col"><span className="visually-hidden">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((row) => {
                    const isSelf = row.id === currentUser?.id
                    const isPending = pendingId === row.id
                    const suspending = row.status === 'ACTIVE'
                    return (
                      <tr key={row.id}>
                        <td>{row.fullName}</td>
                        <td>{row.username}{isSelf && <span className="admin-self"> (tú)</span>}</td>
                        <td>{row.email}</td>
                        <td>{roleLabels[row.role]}</td>
                        <td>
                          <span className={`status-pill ${row.status === 'ACTIVE' ? 'status-active' : 'status-disabled'}`}>
                            {statusLabels[row.status]}
                          </span>
                        </td>
                        <td>{formatDate(row.createdAt)}</td>
                        <td className="admin-actions">
                          <button className={`button ${suspending ? 'button-danger' : 'button-secondary'}`} type="button"
                            onClick={() => void changeStatus(row)}
                            disabled={Boolean(pendingId) || (isSelf && suspending)}
                            title={isSelf && suspending ? 'No puedes suspender tu propia cuenta' : undefined}>
                            {isPending ? 'Guardando...' : suspending ? 'Suspender' : 'Reactivar'}
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              {!hasRows && !isLoading && <p className="admin-empty">No hay cuentas con este filtro.</p>}
            </div>

            {totalPages > 1 && (
              <nav className="pagination" aria-label="Paginación de usuarios">
                <button className="button button-secondary" type="button" disabled={isLoading || page === 0}
                  onClick={() => goToPage(page - 1)}>Anterior</button>
                <span>Página {page + 1} de {totalPages}</span>
                <button className="button button-secondary" type="button" disabled={isLoading || page >= totalPages - 1}
                  onClick={() => goToPage(page + 1)}>Siguiente</button>
              </nav>
            )}
          </>
        )}
      </main>
    </div>
  )
}
