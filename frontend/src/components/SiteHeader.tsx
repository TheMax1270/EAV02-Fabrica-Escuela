import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

export default function SiteHeader() {
  const { user, isAuthenticated, isLoggingOut, logout } = useAuth()

  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link className="brand" to="/" aria-label="DevConnect, inicio">
          <span className="brand-mark" aria-hidden="true">&lt;/&gt;</span>
          <span>DevConnect</span>
        </Link>
        <nav className="site-nav" aria-label="Navegación principal">
          <NavLink to="/" end>Inicio</NavLink>
          {isAuthenticated ? (
            <>
              <NavLink to="/account">Mi cuenta</NavLink>
              <span className="nav-username" title={user?.username}>{user?.username}</span>
              <button className="nav-logout" type="button" onClick={() => void logout()}
                disabled={isLoggingOut}>
                {isLoggingOut ? 'Cerrando...' : 'Cerrar sesión'}
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login">Iniciar sesión</NavLink>
              <Link className="nav-signup" to="/register">Crear cuenta</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
