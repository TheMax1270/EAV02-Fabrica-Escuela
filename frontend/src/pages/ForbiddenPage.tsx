import { Link } from 'react-router-dom'
import SiteHeader from '../components/SiteHeader'

export default function ForbiddenPage() {
  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="auth-main">
        <section className="auth-panel" aria-labelledby="forbidden-title">
          <p className="eyebrow">ACCESO RESTRINGIDO</p>
          <h1 id="forbidden-title">No tienes permisos para ver esta sección</h1>
          <p className="auth-intro">Esta parte de DevConnect está reservada para administradores.</p>
          <Link className="button button-primary" to="/">Volver al inicio</Link>
        </section>
      </main>
    </div>
  )
}
