import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  invalidRefresh,
  readRefreshToken,
  refreshOnce,
  storeRefreshToken,
} from '../session-storage'
import { refreshSession, type LoginResponse } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  refreshSession: vi.fn(),
}))

const mockedRefreshSession = vi.mocked(refreshSession)

const refreshedSession: LoginResponse = {
  accessToken: 'new-access-token',
  refreshToken: 'new-refresh-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: '1', username: 'ada', role: 'DEVELOPER', enabled: true },
}

describe('session storage helpers', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    sessionStorage.clear()
    mockedRefreshSession.mockReset()
  })

  it('stores, reads, and removes the refresh token', () => {
    expect(readRefreshToken()).toBeNull()

    storeRefreshToken('refresh-token')
    expect(readRefreshToken()).toBe('refresh-token')

    storeRefreshToken(null)
    expect(readRefreshToken()).toBeNull()
  })

  it('treats inaccessible browser storage as empty', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('Storage disabled')
    })

    expect(readRefreshToken()).toBeNull()
  })

  it('identifies expired or revoked refresh requests', () => {
    expect(invalidRefresh({ isAxiosError: true, response: { status: 401 } })).toBe(true)
    expect(invalidRefresh({ isAxiosError: true, response: { status: 403 } })).toBe(true)
    expect(invalidRefresh({ isAxiosError: true, response: { status: 500 } })).toBe(false)
    expect(invalidRefresh(new Error('Network error'))).toBe(false)
  })

  it('shares an in-flight refresh and stores the rotated token', async () => {
    let resolveRefresh: (session: LoginResponse) => void = () => {
      throw new Error('Refresh promise was not initialized')
    }
    mockedRefreshSession.mockImplementation(() => new Promise((resolve) => {
      resolveRefresh = resolve
    }))
    storeRefreshToken('current-refresh-token')

    const first = refreshOnce('current-refresh-token')
    const second = refreshOnce('current-refresh-token')

    expect(first).toBe(second)
    expect(mockedRefreshSession).toHaveBeenCalledOnce()

    resolveRefresh(refreshedSession)

    await expect(first).resolves.toEqual(refreshedSession)
    expect(readRefreshToken()).toBe('new-refresh-token')
  })
})
