import { useEffect, useState, type FormEvent } from 'react'
import axios from 'axios'
import { Link } from 'react-router-dom'
import { createProfile, getOwnProfile, updateOwnProfile, type ExperienceLevel, type ProfilePayload } from '../api/profile'
import SiteHeader from '../components/SiteHeader'
import TextField from '../components/TextField'
import { useAuth } from '../context/useAuth'

const levelLabels: Record<ExperienceLevel, string> = {
  JUNIOR: 'Junior',
  SEMI_SENIOR: 'Semi senior',
  SENIOR: 'Senior',
}

const emptyValues: ProfilePayload = {
  biography: '', programmingLanguages: [], technologies: [], experienceLevel: 'JUNIOR',
  githubUrl: null, linkedinUrl: null, portfolioUrl: null,
}

function listText(values: string[]) {
  return values.join(', ')
}

function parseList(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function isValidUrl(value: string) {
  if (!value.trim()) return true
  try {
    const url = new URL(value.trim())
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

export default function ProfilePage() {
  const { user } = useAuth()
  const [values, setValues] = useState<ProfilePayload>(emptyValues)
  const [languages, setLanguages] = useState('')
  const [technologies, setTechnologies] = useState('')
  const [hasProfile, setHasProfile] = useState(false)
  const [isEditing, setIsEditing] = useState(true)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    let cancelled = false

    getOwnProfile().then((profile) => {
      if (cancelled) return
      setValues({ biography: profile.biography, programmingLanguages: profile.programmingLanguages,
        technologies: profile.technologies, experienceLevel: profile.experienceLevel,
        githubUrl: profile.githubUrl, linkedinUrl: profile.linkedinUrl, portfolioUrl: profile.portfolioUrl })
      setLanguages(listText(profile.programmingLanguages))
      setTechnologies(listText(profile.technologies))
      setHasProfile(true)
      setIsEditing(false)
      setError(null)
    }).catch((requestError: unknown) => {
      if (cancelled || (axios.isAxiosError(requestError) && requestError.response?.status === 404)) return
      setError('No pudimos cargar tu perfil técnico.')
    }).finally(() => {
      if (!cancelled) setIsLoading(false)
    })

    return () => { cancelled = true }
  }, [])

  function updateValue(field: keyof ProfilePayload, value: string) {
    setValues((current) => ({ ...current, [field]: value || null }))
    setSaved(false)
    setError(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSaving) return
    const payload: ProfilePayload = { ...values, programmingLanguages: parseList(languages), technologies: parseList(technologies) }
    const invalidUrl = [payload.githubUrl, payload.linkedinUrl, payload.portfolioUrl].some((url) => url && !isValidUrl(url))
    if (!payload.biography.trim() || payload.programmingLanguages.length === 0 || payload.technologies.length === 0) {
      setError('Completa la biografía, los lenguajes y las tecnologías.')
      return
    }
    if (invalidUrl) {
      setError('Los enlaces deben comenzar con http:// o https://.')
      return
    }
    setIsSaving(true)
    setError(null)
    try {
      const result = hasProfile ? await updateOwnProfile(payload) : await createProfile(payload)
      setValues({ biography: result.biography, programmingLanguages: result.programmingLanguages,
        technologies: result.technologies, experienceLevel: result.experienceLevel,
        githubUrl: result.githubUrl, linkedinUrl: result.linkedinUrl, portfolioUrl: result.portfolioUrl })
      setLanguages(listText(result.programmingLanguages))
      setTechnologies(listText(result.technologies))
      setHasProfile(true)
      setIsEditing(false)
      setSaved(true)
    } catch {
      setError('No pudimos guardar tu perfil. Revisa los datos e inténtalo de nuevo.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="auth-main">
        <section className="auth-panel profile-form-panel" aria-labelledby="profile-title">
          <p className="eyebrow">TU IDENTIDAD TÉCNICA</p>
          <h1 id="profile-title">{isEditing ? (hasProfile ? 'Editar perfil técnico' : 'Crear perfil técnico') : 'Perfil técnico'}</h1>
          <p className="auth-intro">{isEditing ? 'Comparte tus habilidades, experiencia y enlaces profesionales.' : 'Así ve la comunidad tu perfil en DevConnect.'}</p>
          {isLoading && <p className="loading-note" role="status">Cargando perfil...</p>}
          {error && <p className="form-error" role="alert">{error}</p>}
          {saved && isEditing && <p className="success-message" role="status">Perfil guardado correctamente.</p>}
          {!isLoading && !isEditing && (
            <div className="public-profile" aria-label="Vista pública del perfil técnico">
              <div className="public-profile-heading">
                <div className="profile-avatar" aria-hidden="true">{user?.username.charAt(0).toUpperCase()}</div>
                <div>
                  <p className="public-profile-kicker">PERFIL PÚBLICO</p>
                  <h2>{user?.username}</h2>
                  <p>{values.experienceLevel && levelLabels[values.experienceLevel]} · DevConnect</p>
                </div>
                <button className="button button-secondary profile-edit-button" type="button"
                  onClick={() => { setIsEditing(true); setSaved(false) }}>
                  Editar perfil
                </button>
              </div>
              <section className="public-profile-section">
                <h3>Sobre mí</h3>
                <p>{values.biography}</p>
              </section>
              <section className="public-profile-section">
                <h3>Lenguajes de programación</h3>
                <div className="skill-list">
                  {values.programmingLanguages.map((language) => <span key={language}>{language}</span>)}
                </div>
              </section>
              <section className="public-profile-section">
                <h3>Tecnologías y frameworks</h3>
                <div className="skill-list">
                  {values.technologies.map((technology) => <span key={technology}>{technology}</span>)}
                </div>
              </section>
              {(values.githubUrl || values.linkedinUrl || values.portfolioUrl) && (
                <section className="public-profile-section public-profile-links">
                  <h3>Enlaces profesionales</h3>
                  {values.githubUrl && <a href={values.githubUrl} target="_blank" rel="noreferrer">GitHub <span aria-hidden="true">↗</span></a>}
                  {values.linkedinUrl && <a href={values.linkedinUrl} target="_blank" rel="noreferrer">LinkedIn <span aria-hidden="true">↗</span></a>}
                  {values.portfolioUrl && <a href={values.portfolioUrl} target="_blank" rel="noreferrer">Portafolio <span aria-hidden="true">↗</span></a>}
                </section>
              )}
            </div>
          )}
          {!isLoading && isEditing && <form onSubmit={handleSubmit} noValidate aria-busy={isSaving}>
            <div className="form-field">
              <label htmlFor="biography">Biografía corta</label>
              <textarea id="biography" value={values.biography} maxLength={500} rows={4}
                onChange={(event) => updateValue('biography', event.target.value)} disabled={isSaving} required />
            </div>
            <TextField name="programmingLanguages" label="Lenguajes de programación" autoComplete="off"
              value={languages} onChange={(value) => { setLanguages(value); setSaved(false) }} disabled={isSaving} />
            <TextField name="technologies" label="Tecnologías y frameworks" autoComplete="off"
              value={technologies} onChange={(value) => { setTechnologies(value); setSaved(false) }} disabled={isSaving} />
            <div className="form-field">
              <label htmlFor="experienceLevel">Nivel de experiencia</label>
              <select id="experienceLevel" value={values.experienceLevel}
                onChange={(event) => updateValue('experienceLevel', event.target.value)} disabled={isSaving}>
                {Object.entries(levelLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </div>
            <TextField name="githubUrl" label="GitHub (opcional)" autoComplete="url"
              value={values.githubUrl ?? ''} onChange={(value) => updateValue('githubUrl', value)} disabled={isSaving} />
            <TextField name="linkedinUrl" label="LinkedIn (opcional)" autoComplete="url"
              value={values.linkedinUrl ?? ''} onChange={(value) => updateValue('linkedinUrl', value)} disabled={isSaving} />
            <TextField name="portfolioUrl" label="Portafolio (opcional)" autoComplete="url"
              value={values.portfolioUrl ?? ''} onChange={(value) => updateValue('portfolioUrl', value)} disabled={isSaving} />
            <button className="button button-primary button-full" type="submit" disabled={isSaving}>
              {isSaving ? 'Guardando...' : hasProfile ? 'Guardar cambios' : 'Crear perfil'}
            </button>
          </form>}
          <p className="auth-switch"><Link to="/account">Volver a mi cuenta</Link></p>
        </section>
      </main>
    </div>
  )
}
