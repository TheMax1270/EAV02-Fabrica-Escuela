import type { HTMLInputTypeAttribute } from 'react'

type TextFieldProps = {
  name: string
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete: string
  type?: HTMLInputTypeAttribute
  error?: string
  maxLength?: number
}

export default function TextField({
  name,
  label,
  value,
  onChange,
  autoComplete,
  type = 'text',
  error,
  maxLength,
}: TextFieldProps) {
  return (
    <div className="form-field">
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        name={name}
        type={type}
        autoComplete={autoComplete}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${name}-error` : undefined}
        maxLength={maxLength}
        required
      />
      {error && <p className="field-error" id={`${name}-error`}>{error}</p>}
    </div>
  )
}
