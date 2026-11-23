import { useEffect, useRef, useState, type FormEvent } from 'react'
import axios from 'axios'
import type { ApiErrorResponse } from '../api/auth'
import {
  getStatistics,
  type StatisticsGroupBy,
  type StatisticsMetric,
  type StatisticsParams,
  type StatisticsResponse,
} from '../api/statistics'
import SiteHeader from '../components/SiteHeader'
import StatisticsChart from '../components/StatisticsChart'

type StatisticsFilters = StatisticsParams
type StatisticsRequest = { params: StatisticsParams; id: number }
type StatisticsResult = { params: StatisticsParams; data: StatisticsResponse }

const metricCards: { key: StatisticsMetric; label: string }[] = [
  { key: 'users', label: 'Usuarios registrados' },
  { key: 'projects', label: 'Proyectos creados' },
  { key: 'posts', label: 'Publicaciones y discusiones' },
  { key: 'comments', label: 'Comentarios realizados' },
]

const metricOptions: { key: StatisticsMetric; label: string }[] = [
  { key: 'users', label: 'Usuarios' },
  { key: 'projects', label: 'Proyectos' },
  { key: 'posts', label: 'Publicaciones' },
  { key: 'comments', label: 'Comentarios' },
]

const numberFormat = new Intl.NumberFormat('es-CO')

function dateInBogota() {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]))
  return `${values.year}-${values.month}-${values.day}`
}

function defaultFilters(): StatisticsFilters {
  const endDate = dateInBogota()
  return { startDate: `${endDate.slice(0, 7)}-01`, endDate, groupBy: 'MONTH' }
}

function isValidDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const daysPerMonth = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  return month >= 1 && month <= 12 && day >= 1 && day <= daysPerMonth[month - 1]
}

function exceedsFiveCalendarYears(startDate: string, endDate: string) {
  const [startYear, startMonth, startDay] = startDate.split('-').map(Number)
  const [endYear, endMonth, endDay] = endDate.split('-').map(Number)
  const maximumYear = startYear + 5
  if (endYear !== maximumYear) return endYear > maximumYear

  const leapYear = maximumYear % 4 === 0 && (maximumYear % 100 !== 0 || maximumYear % 400 === 0)
  const daysPerMonth = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  const maximumDay = Math.min(startDay, daysPerMonth[startMonth - 1])
  return endMonth > startMonth || (endMonth === startMonth && endDay > maximumDay)
}

function validateFilters(filters: StatisticsFilters): string | null {
  if (!filters.startDate || !filters.endDate) {
    return 'Selecciona una fecha inicial y una fecha final.'
  }
  if (!isValidDate(filters.startDate) || !isValidDate(filters.endDate)) {
    return 'Ingresa fechas válidas.'
  }
  if (filters.startDate > filters.endDate) {
    return 'La fecha inicial no puede ser posterior a la fecha final.'
  }
  if (exceedsFiveCalendarYears(filters.startDate, filters.endDate)) {
    return 'El rango no puede superar cinco años.'
  }
  return null
}

function requestErrorMessage(error: unknown) {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return 'No pudimos conectar con el servicio de estadísticas. Verifica tu conexión e inténtalo de nuevo.'
  }

  const status = error.response?.status
  if (status === 400) {
    const details = Object.values(error.response?.data?.errors ?? {}).join(' ')
    return details
      ? `El servidor rechazó la consulta: ${details}`
      : error.response?.data?.message ?? 'La consulta no es válida. Revisa las fechas e inténtalo de nuevo.'
  }
  if (status === 401) return 'Tu sesión ya no está autorizada. Inicia sesión de nuevo e inténtalo.'
  if (status === 403) return 'No tienes permisos para consultar estas estadísticas.'
  return 'No pudimos cargar las estadísticas. Inténtalo de nuevo.'
}

function formatDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day, 12))
  return new Intl.DateTimeFormat('es-CO', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(date)
}

export default function AdminStatisticsPage() {
  const [filters, setFilters] = useState<StatisticsFilters>(defaultFilters)
  const [request, setRequest] = useState<StatisticsRequest>(() => ({
    params: defaultFilters(),
    id: 0,
  }))
  const [result, setResult] = useState<StatisticsResult | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [requestError, setRequestError] = useState<string | null>(null)
  const [filterError, setFilterError] = useState<string | null>(null)
  const [metric, setMetric] = useState<StatisticsMetric>('users')
  const requestId = useRef(0)

  useEffect(() => {
    let cancelled = false
    const isCurrentRequest = () => !cancelled && requestId.current === request.id

    getStatistics(request.params)
      .then((data) => {
        if (isCurrentRequest()) setResult({ params: request.params, data })
      })
      .catch((error: unknown) => {
        if (isCurrentRequest()) setRequestError(requestErrorMessage(error))
      })
      .finally(() => {
        if (isCurrentRequest()) setIsLoading(false)
      })

    return () => { cancelled = true }
  }, [request])

  function updateFilter<K extends keyof StatisticsFilters>(key: K, value: StatisticsFilters[K]) {
    setFilters((current) => ({ ...current, [key]: value }))
    setFilterError(null)
  }

  function submitFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationError = validateFilters(filters)
    setFilterError(validationError)
    if (validationError) return

    setResult(null)
    setRequestError(null)
    setIsLoading(true)
    setRequest({ params: { ...filters }, id: ++requestId.current })
  }

  function retry() {
    setResult(null)
    setRequestError(null)
    setIsLoading(true)
    setRequest((current) => ({ ...current, id: ++requestId.current }))
  }

  const hasNoRecords = result
    && Object.values(result.data.totals).every((count) => count === 0)

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width statistics-main">
        <div className="page-heading">
          <p className="eyebrow">ADMINISTRACIÓN</p>
          <h1>Estadísticas de la plataforma</h1>
          <p>Consulta el crecimiento de usuarios y la actividad registrada en DevConnect.</p>
        </div>

        <form className="statistics-filters" noValidate onSubmit={submitFilters}>
          <div className="form-field">
            <label htmlFor="statistics-start-date">Fecha inicial</label>
            <input id="statistics-start-date" type="date" required value={filters.startDate}
              aria-invalid={Boolean(filterError)}
              aria-describedby={filterError ? 'statistics-filter-error' : undefined}
              onChange={(event) => updateFilter('startDate', event.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="statistics-end-date">Fecha final</label>
            <input id="statistics-end-date" type="date" required value={filters.endDate}
              aria-invalid={Boolean(filterError)}
              aria-describedby={filterError ? 'statistics-filter-error' : undefined}
              onChange={(event) => updateFilter('endDate', event.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="statistics-group-by">Agrupación</label>
            <select id="statistics-group-by" value={filters.groupBy}
              onChange={(event) => updateFilter('groupBy', event.target.value as StatisticsGroupBy)}>
              <option value="WEEK">Semanal</option>
              <option value="MONTH">Mensual</option>
            </select>
          </div>
          <button className="button button-primary statistics-submit" type="submit">
            Consultar estadísticas
          </button>
        </form>

        {filterError && <p className="form-error statistics-error" id="statistics-filter-error" role="alert">{filterError}</p>}
        {isLoading && <p className="loading-note statistics-loading" role="status">Cargando estadísticas...</p>}
        {requestError && (
          <div className="form-error statistics-error" role="alert">
            <span>{requestError}</span>
            <button className="text-button" type="button" onClick={retry}>Reintentar</button>
          </div>
        )}

        {result && !requestError && (
          <section className="statistics-results" aria-label="Resultados de estadísticas">
            <p className="statistics-range" role="status">
              Resultados del {formatDate(result.params.startDate)} al {formatDate(result.params.endDate)}
              {' · Agrupación '}
              {result.params.groupBy === 'WEEK' ? 'semanal' : 'mensual'}
            </p>

            <div className="statistics-cards">
              {metricCards.map(({ key, label }) => (
                <article className="statistics-card" key={key}>
                  <h2>{label}</h2>
                  <p>{numberFormat.format(result.data.totals[key])}</p>
                </article>
              ))}
            </div>

            {hasNoRecords && (
              <p className="statistics-empty" role="status">No hay registros para el período consultado.</p>
            )}

            <section className="statistics-panel" aria-labelledby="statistics-chart-title">
              <div className="statistics-panel-heading">
                <div>
                  <h2 id="statistics-chart-title">Actividad por período</h2>
                  <p>Selecciona una métrica para explorar su evolución.</p>
                </div>
                <div className="form-field statistics-metric-filter">
                  <label htmlFor="statistics-metric">Métrica de la gráfica</label>
                  <select id="statistics-metric" value={metric}
                    onChange={(event) => setMetric(event.target.value as StatisticsMetric)}>
                    {metricOptions.map((option) => (
                      <option key={option.key} value={option.key}>{option.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <StatisticsChart series={result.data.series} groupBy={result.data.groupBy} metric={metric} />

              <div className="statistics-table-wrap">
                <table className="admin-table statistics-table">
                  <caption className="visually-hidden">Conteos de cada métrica por período</caption>
                  <thead>
                    <tr>
                      <th scope="col">Período</th>
                      <th scope="col">Usuarios</th>
                      <th scope="col">Proyectos</th>
                      <th scope="col">Publicaciones</th>
                      <th scope="col">Comentarios</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.data.series.map((period) => (
                      <tr key={period.period}>
                        <th scope="row"><time dateTime={period.period}>{period.period}</time></th>
                        <td>{numberFormat.format(period.users)}</td>
                        <td>{numberFormat.format(period.projects)}</td>
                        <td>{numberFormat.format(period.posts)}</td>
                        <td>{numberFormat.format(period.comments)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {result.data.series.length === 0 && (
                  <p className="admin-empty">No hay períodos para mostrar en este rango.</p>
                )}
              </div>
            </section>
          </section>
        )}
      </main>
    </div>
  )
}
