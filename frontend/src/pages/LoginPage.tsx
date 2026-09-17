import { useState, type FormEvent } from 'react'
import axios from 'axios'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import TextField from '../components/TextField'
import SiteHeader from '../components/SiteHeader'
import { useAuth } from '../context/useAuth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login, isAuthenticated } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ identifier?: string; password?: string }>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/account" replace />

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return
    setFormError(null)
    const nextErrors = {
      identifier: identifier.trim() ? undefined : 'Ingresa tu usuario o correo.',
      password: password ? undefined : 'Ingresa tu contraseña.',
    }
    setErrors(nextErrors)
    if (nextErrors.identifier || nextErrors.password) return

    setIsSubmitting(true)
    try {
      await login({ identifier: identifier.trim(), password })
      setPassword('')
      navigate('/account', { replace: true })
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        setFormError('Usuario/correo o contraseña incorrectos.')
      } else if (axios.isAxiosError(error) && error.response?.status === 403) {
        setFormError('Tu cuenta no está habilitada.')
      } else {
        setFormError('No fue posible iniciar sesión. Inténtalo nuevamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="auth-main">
        <section className="auth-panel" aria-labelledby="login-title">
          <p className="eyebrow">TU ESPACIO DEV</p>
          <h1 id="login-title">Bienvenido de nuevo</h1>
          <p className="auth-intro">Inicia sesión para continuar en la comunidad.</p>
          {formError && <p className="form-error" role="alert">{formError}</p>}
          <form onSubmit={handleSubmit} noValidate aria-busy={isSubmitting}>
            <TextField name="identifier" label="Usuario o correo" autoComplete="username"
              value={identifier} onChange={(value) => { setIdentifier(value); setErrors((current) => ({ ...current, identifier: undefined })); setFormError(null) }}
              error={errors.identifier} disabled={isSubmitting} />
            <TextField name="password" label="Contraseña" type="password" autoComplete="current-password"
              value={password} onChange={(value) => { setPassword(value); setErrors((current) => ({ ...current, password: undefined })); setFormError(null) }}
              error={errors.password} disabled={isSubmitting} />
            <button className="button button-primary button-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Iniciando sesión...' : 'Iniciar sesión'}
            </button>
          </form>
          <p className="auth-switch">¿No tienes una cuenta? <Link to="/register">Crear cuenta</Link></p>
        </section>
      </main>
    </div>
  )
}
