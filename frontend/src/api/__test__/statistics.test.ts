import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../client'
import { getStatistics, type StatisticsResponse } from '../statistics'

vi.mock('../client', () => ({
  api: { get: vi.fn() },
}))

const response: StatisticsResponse = {
  startDate: '2026-11-01',
  endDate: '2026-11-30',
  groupBy: 'WEEK',
  totals: { users: 1, projects: 2, posts: 3, comments: 4 },
  series: [],
}

describe('getStatistics', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockReset().mockResolvedValue({ data: response })
  })

  it('sends the date range and grouping to the admin statistics endpoint', async () => {
    const params = { startDate: '2026-11-01', endDate: '2026-11-30', groupBy: 'WEEK' as const }

    await expect(getStatistics(params)).resolves.toEqual(response)

    expect(api.get).toHaveBeenCalledWith('/api/v1/admin/statistics', { params })
  })
})
