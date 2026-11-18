import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { getProfileByUser, type ExperienceLevel, type Profile } from '../api/profile'
import { reportProfile } from '../api/report'
import SiteHeader from '../components/SiteHeader'
import ReportButton from '../components/ReportButton'
import FollowButton from '../components/FollowButton'
import FollowListPanel from '../components/FollowListPanel'
import BlockButton from '../components/BlockButton'
import { profileReportStatus } from '../api/report'
import { useAuth } from '../context/useAuth'
import { ChevronDown, Send } from 'lucide-react'

const levelLabels: Record<ExperienceLevel, string> = {
  JUNIOR: 'Junior', SEMI_SENIOR: 'Semi senior', SENIOR: 'Senior',
}

export default function DeveloperProfilePage() {
  const { userId } = useParams()
  return userId ? <DeveloperProfileView key={userId} userId={userId} /> : null
}

function DeveloperProfileView({ userId }: Readonly<{ userId: string }>) {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [followReload, setFollowReload] = useState(0)
  const refreshFollows = () => setFollowReload((value) => value + 1)

  useEffect(() => {
    const controller = new AbortController()
    window.scrollTo(0, 0)
    getProfileByUser(userId, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setProfile(result); setError(null); setNotFound(false) }
    }).catch((failure: unknown) => {
      if (controller.signal.aborted) return
      if (axios.isAxiosError(failure) && failure.response?.status === 404) setNotFound(true)
      else setError('No pudimos cargar este perfil.')
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [userId])

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="auth-main">
        <section className="auth-panel profile-form-panel" aria-labelledby="dev-profile-title">
          <p className="eyebrow">PERFIL DE LA COMUNIDAD</p>
          {loading && <p className="loading-note" role="status">Cargando perfil...</p>}
          {notFound && (
            <div className="public-profile">
              <div className="profile-public-header">
                <p className="profile-missing-note">Este desarrollador aún no ha creado su perfil técnico.</p>
                <ProfileActions userId={userId} reloadKey={followReload} onChange={refreshFollows} />
              </div>
              <FollowButton countsOnly userId={userId} reloadKey={followReload} />
              <FollowListPanel userId={userId} reloadKey={followReload} />
            </div>
          )}
          {error && <p className="form-error" role="alert">{error}</p>}
          {profile && (
            <div className="public-profile">
              <div className="profile-public-header">
                <div className="public-profile-heading">
                  <div className="profile-avatar" aria-hidden="true">{profile.username.charAt(0).toUpperCase()}</div>
                  <div>
                    <h2 id="dev-profile-title">{profile.username}</h2>
                    <p>{levelLabels[profile.experienceLevel]} · DevConnect</p>
                  </div>
                </div>
                <ProfileActions userId={profile.userId} profileId={profile.id} reloadKey={followReload}
                  onChange={refreshFollows} />
              </div>
              <FollowButton countsOnly userId={profile.userId} reloadKey={followReload} />
              <section className="public-profile-section">
                <h3>Sobre mí</h3>
                <p>{profile.biography}</p>
              </section>
              <section className="public-profile-section">
                <h3>Lenguajes de programación</h3>
                <div className="skill-list">
                  {profile.programmingLanguages.map((language) => <span key={language}>{language}</span>)}
                </div>
              </section>
              <section className="public-profile-section">
                <h3>Tecnologías y frameworks</h3>
                <div className="skill-list">
                  {profile.technologies.map((technology) => <span key={technology}>{technology}</span>)}
                </div>
              </section>
              <FollowListPanel userId={profile.userId} reloadKey={followReload} />
            </div>
          )}
          <p className="auth-switch"><Link to="/posts">Volver a publicaciones</Link></p>
        </section>
      </main>
    </div>
  )
}
// Seguir, mensaje y menú "Más": iguales tengan o no perfil técnico. Reportar requiere un perfil que reportar.
function ProfileActions({ userId, profileId, reloadKey, onChange }: Readonly<{
  userId: string; profileId?: string; reloadKey: number; onChange: () => void
}>) {
  const { user } = useAuth()
  const [moreOpen, setMoreOpen] = useState(false)
  const moreRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!moreOpen) return undefined
    function closeOnOutsideClick(event: PointerEvent) {
      if (moreRef.current && !moreRef.current.contains(event.target as Node)) setMoreOpen(false)
    }
    document.addEventListener('pointerdown', closeOnOutsideClick)
    return () => document.removeEventListener('pointerdown', closeOnOutsideClick)
  }, [moreOpen])

  if (user?.id === userId) return null
  return <div className="profile-action-row">
    <FollowButton compact userId={userId} reloadKey={reloadKey} onChange={onChange} />
    <Link className="button message-button" to={`/messages/${userId}`}><Send size={20} strokeWidth={1.8} aria-hidden="true" /> Enviar mensaje</Link>
    <div className="profile-more" ref={moreRef}>
      <button className="button button-secondary more-button" type="button" aria-expanded={moreOpen}
        aria-haspopup="menu" onClick={() => setMoreOpen((value) => !value)}>
        Más <ChevronDown size={18} strokeWidth={1.8} aria-hidden="true" />
      </button>
      {moreOpen && <div className="profile-more-menu" role="menu">
        {profileId && <ReportButton statusCheck={() => profileReportStatus(profileId)}
          onSubmit={(payload) => reportProfile(profileId, payload)} />}
        <BlockButton userId={userId} compact reloadKey={reloadKey}
          onChange={() => { onChange(); setMoreOpen(false) }} />
      </div>}
    </div>
  </div>
}
