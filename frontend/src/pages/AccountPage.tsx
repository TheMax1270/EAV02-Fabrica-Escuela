import { useCallback, useEffect, useState } from 'react'
import { getCurrentUser, type AuthUser } from '../api/auth'
import SiteHeader from '../components/SiteHeader'
import { useAuth } from '../context/useAuth'
import { Link } from 'react-router-dom'

const roleLabels: Record<AuthUser['role'], string> = {
  DEVELOPER: 'Desarrollador',
  ADMIN: 'Administrador',
}

export default function AccountPage() {
  const { user, updateUser, logout, isLoggingOut } = useAuth()
  const [profile, setProfile] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)

  const retry = useCallback(() => setReload((current) => current + 1), [])

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then((current) => {
      if (cancelled) return
      setProfile(current)
      updateUser(current)
      setError(null)
    }).catch(() => {
      if (!cancelled) setError('No pudimos cargar tu cuenta. Intenta de nuevo.')
    }).finally(() => {
      if (!cancelled) setIsLoading(false)
    })
    return () => { cancelled = true }
  }, [reload, updateUser])

  const current = profile ?? user

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width">
        <div className="page-heading">
          <p className="eyebrow">ESPACIO PERSONAL</p>
          <h1>Mi cuenta</h1>
          <p>Tu identidad dentro de DevConnect.</p>
        </div>
        {isLoading && <p className="loading-note" role="status">Cargando cuenta...</p>}
        {error && (
          <div className="form-error" role="alert">
            <span>{error}</span>
            <button className="text-button" type="button" onClick={() => { setIsLoading(true); retry() }}>
              Reintentar
            </button>
          </div>
        )}
        {current && !error && (
          <section className="profile-panel" aria-label="Datos de la cuenta">
            <div className="profile-topline">
              <span className="profile-avatar" aria-hidden="true">{current.username.charAt(0).toUpperCase()}</span>
              <div>
                <h2>{current.username}</h2>
                <p>{roleLabels[current.role]}</p>
              </div>
              <span className={`status-pill ${current.enabled ? 'status-active' : 'status-disabled'}`}>
                {current.enabled ? 'Cuenta activa' : 'Cuenta no habilitada'}
              </span>
            </div>
            <div className="profile-detail">
              <span>Rol en la comunidad</span>
              <strong>{roleLabels[current.role]}</strong>
            </div>
            <Link className="button button-secondary" to="/projects">Ver mis proyectos</Link>
            <Link className="button button-secondary" to="/profile">Crear o editar perfil técnico</Link>
            <button className="button button-secondary" type="button" onClick={() => void logout()}
              disabled={isLoggingOut}>
              {isLoggingOut ? 'Cerrando sesión...' : 'Cerrar sesión'}
            </button>
          </section>
        )}
      </main>
    </div>
  )
}
