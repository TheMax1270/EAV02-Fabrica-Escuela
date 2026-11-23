import type { StatisticsGroupBy, StatisticsMetric, StatisticsPeriod } from '../api/statistics'

type StatisticsChartProps = {
  series: StatisticsPeriod[]
  groupBy: StatisticsGroupBy
  metric: StatisticsMetric
}

const metricLabels: Record<StatisticsMetric, string> = {
  users: 'usuarios',
  projects: 'proyectos',
  posts: 'publicaciones',
  comments: 'comentarios',
}

const numberFormat = new Intl.NumberFormat('es-CO')

function formatPeriod(period: string, groupBy: StatisticsGroupBy) {
  const [year, month, day] = period.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day, 12))
  const options: Intl.DateTimeFormatOptions = groupBy === 'WEEK'
    ? { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' }
    : { month: 'short', year: 'numeric', timeZone: 'UTC' }
  return new Intl.DateTimeFormat('es-CO', options).format(date)
}

export default function StatisticsChart({ series, groupBy, metric }: StatisticsChartProps) {
  const maximum = Math.max(0, ...series.map((item) => item[metric]))
  const chartWidth = Math.max(600, series.length * 64)

  return (
    <figure className="statistics-chart" aria-label={`Gráfica de barras de ${metricLabels[metric]} por período`}>
      <div className="statistics-chart-scroll" role="region"
        aria-label="Gráfica de actividad. Desplázate horizontalmente para consultar todos los períodos."
        tabIndex={0}>
        <div className="statistics-chart-bars" style={{ width: `${chartWidth}px` }}>
          {series.map((item) => {
            const value = item[metric]
            const height = maximum === 0 ? 0 : (value / maximum) * 100
            return (
              <div className="statistics-chart-column" key={item.period}>
                <span className="statistics-chart-value">{numberFormat.format(value)}</span>
                <div className="statistics-chart-track" aria-hidden="true">
                  <div className="statistics-chart-bar" style={{ height: `${height}%` }} />
                </div>
                <span className="statistics-chart-period">{formatPeriod(item.period, groupBy)}</span>
              </div>
            )
          })}
        </div>
      </div>
      <figcaption className="visually-hidden">
        Valores de {metricLabels[metric]} por período. La tabla siguiente contiene todas las métricas.
      </figcaption>
    </figure>
  )
}
