import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <span className="brand">EAV02</span>
      </header>
      <main className="home-main">
        <h1>EAV02 - Fábrica Escuela</h1>
        <Link className="primary-link" to="/register">Crear cuenta</Link>
      </main>
    </div>
  )
}
