import { useState, type FormEvent } from 'react'
import axios from 'axios'
import { Link } from 'react-router-dom'
import { registerUser, type ApiErrorResponse, type RegistrationPayload } from '../api/auth'
import TextField from '../components/TextField'
import SiteHeader from '../components/SiteHeader'

type FieldName = keyof RegistrationPayload
type FieldErrors = Partial<Record<FieldName, string>>

const initialValues: RegistrationPayload = {
  fullName: '',
  username: '',
  email: '',
  password: '',
  passwordConfirmation: '',
}

const fieldNames: FieldName[] = [
  'fullName', 'username', 'email', 'password', 'passwordConfirmation',
]

function validate(values: RegistrationPayload): FieldErrors {
  const errors: FieldErrors = {}

  if (!values.fullName.trim()) errors.fullName = 'Ingresa tu nombre completo.'
  if (!values.username.trim()) errors.username = 'Ingresa un nombre de usuario.'
  if (!values.email.trim()) {
    errors.email = 'Ingresa tu correo electrónico.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errors.email = 'Ingresa un correo electrónico válido.'
  }
  if (!values.password) {
    errors.password = 'Ingresa una contraseña.'
  } else if (values.password.length < 8) {
    errors.password = 'La contraseña debe tener al menos 8 caracteres.'
  } else if (!/[0-9]/.test(values.password)) {
    errors.password = 'La contraseña debe contener al menos un número.'
  } else if (new TextEncoder().encode(values.password).length > 72) {
    errors.password = 'La contraseña no debe superar 72 bytes UTF-8.'
  }
  if (!values.passwordConfirmation) {
    errors.passwordConfirmation = 'Confirma tu contraseña.'
  } else if (values.password !== values.passwordConfirmation) {
    errors.passwordConfirmation = 'Las contraseñas no coinciden.'
  }

  return errors
}

function backendFieldErrors(data: unknown): FieldErrors {
  if (!data || typeof data !== 'object' || !('errors' in data)) return {}
  const reported = data.errors
  if (!reported || typeof reported !== 'object') return {}

  const errors: FieldErrors = {}
  for (const field of fieldNames) {
    const message = (reported as Record<string, unknown>)[field]
    if (typeof message === 'string') errors[field] = message
  }
  return errors
}

function backendMessage(data: unknown): string | null {
  if (!data || typeof data !== 'object' || !('message' in data)) return null
  return typeof data.message === 'string' ? data.message : null
}

export default function RegisterPage() {
  const [values, setValues] = useState<RegistrationPayload>(initialValues)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [createdUsername, setCreatedUsername] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField(field: FieldName, value: string) {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
    setFormError(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setCreatedUsername(null)
    setFormError(null)
    const nextErrors = validate(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setIsSubmitting(true)
    try {
      const response = await registerUser({
        fullName: values.fullName.trim(),
        username: values.username.trim(),
        email: values.email.trim(),
        password: values.password,
        passwordConfirmation: values.passwordConfirmation,
      })
      setCreatedUsername(response.username)
      setValues((current) => ({ ...current, password: '', passwordConfirmation: '' }))
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error) && error.response) {
        const { status, data } = error.response
        if (status === 400) {
          const reported = backendFieldErrors(data)
          setErrors(reported)
          if (Object.keys(reported).length === 0) {
            setFormError(backendMessage(data) ?? 'Revisa los datos del formulario.')
          }
        } else if (status === 409) {
          setErrors(backendFieldErrors(data))
          setFormError('El correo o nombre de usuario ya está registrado.')
        } else {
          setFormError('No pudimos completar el registro. Intenta de nuevo.')
        }
      } else {
        setFormError('No pudimos completar el registro. Intenta de nuevo.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="auth-main">
        <section className="auth-panel" aria-labelledby="register-title">
          <p className="eyebrow">ÚNETE A DEVCONNECT</p>
          <h1 id="register-title">Crear cuenta</h1>
          <p className="auth-intro">Únete a la comunidad de desarrolladores.</p>
          {createdUsername && (
            <p className="success-message" role="status">
              Cuenta creada para <strong>{createdUsername}</strong>.
            </p>
          )}
          {formError && <p className="form-error" role="alert">{formError}</p>}

          <form onSubmit={handleSubmit} noValidate aria-busy={isSubmitting}>
            <TextField name="fullName" label="Nombre completo" autoComplete="name"
              value={values.fullName} onChange={(value) => updateField('fullName', value)}
              error={errors.fullName} maxLength={150} disabled={isSubmitting} />
            <TextField name="username" label="Nombre de usuario" autoComplete="username"
              value={values.username} onChange={(value) => updateField('username', value)}
              error={errors.username} maxLength={50} disabled={isSubmitting} />
            <TextField name="email" label="Correo electrónico" type="email" autoComplete="email"
              value={values.email} onChange={(value) => updateField('email', value)}
              error={errors.email} maxLength={254} disabled={isSubmitting} />
            <TextField name="password" label="Contraseña" type="password" autoComplete="new-password"
              value={values.password} onChange={(value) => updateField('password', value)}
              error={errors.password} disabled={isSubmitting} />
            <TextField name="passwordConfirmation" label="Confirmar contraseña" type="password"
              autoComplete="new-password" value={values.passwordConfirmation}
              onChange={(value) => updateField('passwordConfirmation', value)}
              error={errors.passwordConfirmation} disabled={isSubmitting} />
            <button className="button button-primary button-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Registrando...' : 'Registrarse'}
            </button>
          </form>
          <p className="auth-switch">¿Ya tienes una cuenta? <Link to="/login">Inicia sesión</Link></p>
        </section>
      </main>
    </div>
  )
}
