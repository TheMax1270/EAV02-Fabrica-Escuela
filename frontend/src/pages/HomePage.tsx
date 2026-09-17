import { Link } from 'react-router-dom'
import SiteHeader from '../components/SiteHeader'
import { useAuth } from '../context/useAuth'

const highlights = [
  { number: '01', title: 'Perfil técnico', description: 'Un lugar para mostrar quién eres y lo que sabes construir.' },
  { number: '02', title: 'Comparte proyectos', description: 'Dale visibilidad al trabajo que merece ser visto.' },
  { number: '03', title: 'Conecta con la comunidad', description: 'Encuentra desarrolladores con intereses afines.' },
]

export default function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="app-shell">
      <SiteHeader />
      <main>
        <section className="home-hero content-width" aria-labelledby="home-title">
          <div className="hero-copy">
            <p className="eyebrow"><span className="eyebrow-line" /> UN ESPACIO PARA CREAR JUNTOS</p>
            <h1 id="home-title">Red social para <span>desarrolladores</span></h1>
            <p className="hero-description">Comparte tus proyectos, construye tu perfil técnico y conecta con otros desarrolladores.</p>
            <div className="hero-actions">
              {isAuthenticated ? (
                <Link className="button button-primary" to="/account">Ir a mi cuenta <span aria-hidden="true">→</span></Link>
              ) : (
                <>
                  <Link className="button button-primary" to="/register">Crear cuenta <span aria-hidden="true">→</span></Link>
                  <Link className="button button-secondary" to="/login">Iniciar sesión</Link>
                </>
              )}
            </div>
          </div>
          <div className="hero-visual" aria-hidden="true">
            <div className="code-window">
              <div className="code-toolbar"><span /><span /><span /><small>community.ts</small></div>
              <div className="code-lines">
                <div><i>01</i><code><b>const</b> developer = {'{'}</code></div>
                <div><i>02</i><code>  name: <em>'tu nombre'</em>,</code></div>
                <div><i>03</i><code>  stack: [<em>'ideas'</em>, <em>'código'</em>],</code></div>
                <div><i>04</i><code>  community: <em>'DevConnect'</em></code></div>
                <div><i>05</i><code>{'}'}</code></div>
                <div><i>06</i><code>&nbsp;</code></div>
                <div><i>07</i><code><b>connect</b>(developer)</code></div>
              </div>
              <div className="code-output"><span className="terminal-prompt">›</span> building something together<span className="cursor-mark">_</span></div>
            </div>
            <div className="visual-caption"><span className="caption-dot" /> IDEAS → CÓDIGO → COMUNIDAD</div>
          </div>
        </section>
        <section className="purpose-band" aria-labelledby="purpose-title">
          <div className="content-width">
            <div className="section-heading">
              <p className="eyebrow">CONSTRUYE TU RED</p>
              <h2 id="purpose-title">Más que un repositorio.</h2>
            </div>
            <div className="highlight-grid">
              {highlights.map((item) => (
                <div className="highlight" key={item.number}>
                  <span className="highlight-number">{item.number}</span>
                  <h3>{item.title}</h3>
                  <p>{item.description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}
