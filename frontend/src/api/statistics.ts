import { api } from './client'

export type StatisticsGroupBy = 'WEEK' | 'MONTH'

export type StatisticsMetric = 'users' | 'projects' | 'posts' | 'comments'

export type StatisticsParams = {
  startDate: string
  endDate: string
  groupBy: StatisticsGroupBy
}

export type StatisticsTotals = Record<StatisticsMetric, number>

export type StatisticsPeriod = StatisticsTotals & {
  period: string
}

export type StatisticsResponse = {
  startDate: string
  endDate: string
  groupBy: StatisticsGroupBy
  totals: StatisticsTotals
  series: StatisticsPeriod[]
}

export async function getStatistics(params: StatisticsParams): Promise<StatisticsResponse> {
  const response = await api.get<StatisticsResponse>('/api/v1/admin/statistics', { params })
  return response.data
}
