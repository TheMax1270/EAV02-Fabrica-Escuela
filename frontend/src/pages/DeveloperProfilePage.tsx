import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { getProfileByUser, type ExperienceLevel, type Profile } from '../api/profile'
import { reportProfile } from '../api/report'
import SiteHeader from '../components/SiteHeader'
import ReportButton from '../components/ReportButton'
import FollowButton from '../components/FollowButton'
import FollowListPanel from '../components/FollowListPanel'
import { profileReportStatus } from '../api/report'
import { useAuth } from '../context/useAuth'

const levelLabels: Record<ExperienceLevel, string> = {
  JUNIOR: 'Junior', SEMI_SENIOR: 'Semi senior', SENIOR: 'Senior',
}

export default function DeveloperProfilePage() {
  const { userId } = useParams()
  return userId ? <DeveloperProfileView key={userId} userId={userId} /> : null
}

function DeveloperProfileView({ userId }: Readonly<{ userId: string }>) {
  const { user } = useAuth()
  const [profile, setProfile] = useState<Profile | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [followReload, setFollowReload] = useState(0)

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
              <p>Este desarrollador aún no ha creado su perfil técnico.</p>
              <FollowButton userId={userId} onChange={() => setFollowReload((value) => value + 1)} />
              <FollowListPanel userId={userId} reloadKey={followReload} />
            </div>
          )}
          {error && <p className="form-error" role="alert">{error}</p>}
          {profile && (
            <div className="public-profile">
              <div className="public-profile-heading">
                <div className="profile-avatar" aria-hidden="true">{profile.username.charAt(0).toUpperCase()}</div>
                <div>
                  <h2 id="dev-profile-title">{profile.username}</h2>
                  <p>{levelLabels[profile.experienceLevel]} · DevConnect</p>
                </div>
              </div>
              <FollowButton userId={profile.userId} onChange={() => setFollowReload((value) => value + 1)} />
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
              {user?.id !== profile.userId && <ReportButton statusCheck={() => profileReportStatus(profile.id)}
                onSubmit={(payload) => reportProfile(profile.id, payload)} />}
            </div>
          )}
          <p className="auth-switch"><Link to="/posts">Volver a publicaciones</Link></p>
        </section>
      </main>
    </div>
  )
}