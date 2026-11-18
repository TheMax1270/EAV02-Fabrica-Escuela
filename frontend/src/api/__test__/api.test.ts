import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { api } from '../client'
import {
  getCurrentUser,
  loginUser,
  logoutUser,
  refreshSession,
  registerUser,
} from '../auth'
import { listUsers, reactivateUser, suspendUser } from '../admin'
import { getFollowedAmong, getFollowSummary, listFollows, setFollowing } from '../follow'
import {
  deleteComment,
  getCommentCount,
  getLikes,
  getReplyCount,
  interactionError,
  listComments,
  sendComment,
  setLike,
  targetPath,
} from '../interaction'
import {
  dismissReport,
  listPendingReports,
  listResolvedReports,
  removeReportedContent,
  restoreReport,
  reviewReport,
} from '../moderation'
import { createPost, deletePost, listPosts, resolvePost } from '../post'
import { createProfile, getOwnProfile, getProfileByUser, updateOwnProfile } from '../profile'
import { createProject, getMyProjects, getProject, updateProject } from '../project'
import {
  commentReportStatus,
  postReportStatus,
  profileReportStatus,
  reportError,
  reportComment,
  reportPost,
  reportProfile,
} from '../report'

const { mockedApi } = vi.hoisted(() => ({
  mockedApi: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('../client', () => ({ api: mockedApi }))

const mockGet = vi.mocked(api.get)
const mockPost = vi.mocked(api.post)
const mockPut = vi.mocked(api.put)
const mockPatch = vi.mocked(api.patch)
const mockDelete = vi.mocked(api.delete)

function respondWith(data: unknown) {
  mockGet.mockResolvedValue({ data } as never)
  mockPost.mockResolvedValue({ data } as never)
  mockPut.mockResolvedValue({ data } as never)
  mockPatch.mockResolvedValue({ data } as never)
}

describe('frontend API modules', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    mockGet.mockReset()
    mockPost.mockReset()
    mockPut.mockReset()
    mockPatch.mockReset()
    mockDelete.mockReset()
    respondWith({ id: 'resource-1' })
  })

  it('sends authentication requests to the expected endpoints', async () => {
    const login = { identifier: 'ada', password: 'secret123' }
    const registration = {
      fullName: 'Ada Lovelace',
      username: 'ada',
      email: 'ada@example.com',
      password: 'secret123',
      passwordConfirmation: 'secret123',
    }

    await loginUser(login)
    await refreshSession('refresh-token')
    await logoutUser('refresh-token')
    await getCurrentUser()
    await registerUser(registration)

    expect(mockPost).toHaveBeenNthCalledWith(1, '/api/v1/auth/login', login)
    expect(mockPost).toHaveBeenNthCalledWith(2, '/api/v1/auth/refresh', { refreshToken: 'refresh-token' })
    expect(mockPost).toHaveBeenNthCalledWith(3, '/api/v1/auth/logout', { refreshToken: 'refresh-token' })
    expect(mockGet).toHaveBeenCalledWith('/api/v1/auth/me')
    expect(mockPost).toHaveBeenNthCalledWith(4, '/api/v1/auth/register', registration)
  })

  it('lists and updates admin users with the requested filters', async () => {
    const params = { page: 1, size: 20, status: 'ACTIVE' as const }

    await listUsers(params)
    await suspendUser('user-1')
    await reactivateUser('user-1')

    expect(mockGet).toHaveBeenCalledWith('/api/v1/admin/users', { params })
    expect(mockPost).toHaveBeenNthCalledWith(1, '/api/v1/admin/users/user-1/suspend')
    expect(mockPost).toHaveBeenNthCalledWith(2, '/api/v1/admin/users/user-1/reactivate')
  })

  it('handles follow summaries, lists, and follow state changes', async () => {
    const signal = new AbortController().signal

    await getFollowSummary('user-1', signal)
    await listFollows('user-1', 'followers', 2, signal)
    await getFollowedAmong(['user-1', 'user-2'], signal)
    await setFollowing('user-1', true)
    await setFollowing('user-1', false)

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/v1/users/user-1/follow-summary', { signal })
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/v1/users/user-1/followers', {
      params: { page: 2, size: 20 },
      signal,
    })
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/v1/me/following', {
      params: { userIds: 'user-1,user-2' },
      signal,
    })
    expect(mockPut).toHaveBeenCalledWith('/api/v1/users/user-1/followers/me')
    expect(mockDelete).toHaveBeenCalledWith('/api/v1/users/user-1/followers/me')
  })

  it('builds interaction paths and sends comment and like requests', async () => {
    const target = { kind: 'posts' as const, id: 'post-1' }
    const signal = new AbortController().signal

    expect(targetPath(target)).toBe('/api/v1/posts/post-1')
    await listComments('/api/v1/posts/post-1/comments', 3, signal)
    await getCommentCount(target)
    await getReplyCount('comment-1')
    await sendComment('/api/v1/posts/post-1/comments', 'Hello')
    await deleteComment('comment-1')
    await getLikes(target, signal)
    await setLike(target, true)
    await setLike(target, false)

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/v1/posts/post-1/comments', {
      params: { page: 3, size: 20 },
      signal,
    })
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/v1/posts/post-1/comments/count')
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/v1/comments/comment-1/replies', {
      params: { page: 0, size: 1 },
    })
    expect(mockPost).toHaveBeenCalledWith('/api/v1/posts/post-1/comments', { content: 'Hello' })
    expect(mockDelete).toHaveBeenCalledWith('/api/v1/comments/comment-1')
    expect(mockGet).toHaveBeenNthCalledWith(4, '/api/v1/posts/post-1/likes', { signal })
    expect(mockPut).toHaveBeenCalledWith('/api/v1/posts/post-1/likes/me')
    expect(mockDelete).toHaveBeenLastCalledWith('/api/v1/posts/post-1/likes/me')
  })

  it('lists reports and applies each moderation action', async () => {
    await listPendingReports(0, 10)
    await listResolvedReports(1, 10)
    await reviewReport('report-1')
    await restoreReport('report-1')
    await removeReportedContent('report-1')
    await dismissReport('report-1')

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/v1/admin/reports', {
      params: { page: 0, size: 10 },
    })
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/v1/admin/reports/resolved', {
      params: { page: 1, size: 10 },
    })
    expect(mockPatch).toHaveBeenNthCalledWith(1, '/api/v1/admin/reports/report-1/review')
    expect(mockPatch).toHaveBeenNthCalledWith(2, '/api/v1/admin/reports/report-1/restore')
    expect(mockPatch).toHaveBeenNthCalledWith(3, '/api/v1/admin/reports/report-1/remove')
    expect(mockPatch).toHaveBeenNthCalledWith(4, '/api/v1/admin/reports/report-1/dismiss')
  })

  it('lists and changes posts using their API methods', async () => {
    const payload = { type: 'GENERAL' as const, content: 'Hello community' }
    const signal = new AbortController().signal

    await listPosts(2, 15, signal)
    await createPost(payload)
    await resolvePost('post-1')
    await deletePost('post-1')

    expect(mockGet).toHaveBeenCalledWith('/api/v1/posts', {
      params: { page: 2, size: 15 },
      signal,
    })
    expect(mockPost).toHaveBeenCalledWith('/api/v1/posts', payload)
    expect(mockPatch).toHaveBeenCalledWith('/api/v1/posts/post-1/resolve')
    expect(mockDelete).toHaveBeenCalledWith('/api/v1/posts/post-1')
  })

  it('loads and saves own and public profiles', async () => {
    const payload = {
      biography: 'Engineer',
      programmingLanguages: ['Java'],
      technologies: ['Spring'],
      experienceLevel: 'SENIOR' as const,
      githubUrl: null,
      linkedinUrl: null,
      portfolioUrl: null,
    }
    const signal = new AbortController().signal

    await getOwnProfile()
    await createProfile(payload)
    await updateOwnProfile(payload)
    await getProfileByUser('user-1', signal)

    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/v1/profile/me')
    expect(mockPost).toHaveBeenCalledWith('/api/v1/profile/me', payload)
    expect(mockPut).toHaveBeenCalledWith('/api/v1/profile/me', payload)
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/v1/profiles/user/user-1', { signal })
  })

  it('loads and changes projects', async () => {
    const payload = {
      title: 'DevConnect',
      description: 'Community',
      technologies: ['React'],
      status: 'IN_PROGRESS' as const,
      repositoryUrl: null,
    }
    const signal = new AbortController().signal

    await createProject(payload)
    await getMyProjects()
    await getProject('project-1', signal)
    await updateProject('project-1', payload)

    expect(mockPost).toHaveBeenCalledWith('/api/v1/projects', payload)
    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/v1/projects/me')
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/v1/projects/project-1', { signal })
    expect(mockPut).toHaveBeenCalledWith('/api/v1/projects/project-1', payload)
  })

  it('creates content reports and checks report status', async () => {
    const payload = { reason: 'SPAM' as const, details: 'Repeated content' }

    await reportPost('post-1', payload)
    await reportComment('comment-1', payload)
    await reportProfile('profile-1', payload)
    await postReportStatus('post-1')
    await commentReportStatus('comment-1')
    await profileReportStatus('profile-1')

    expect(mockPost).toHaveBeenNthCalledWith(1, '/api/v1/posts/post-1/reports', payload)
    expect(mockPost).toHaveBeenNthCalledWith(2, '/api/v1/comments/comment-1/reports', payload)
    expect(mockPost).toHaveBeenNthCalledWith(3, '/api/v1/profiles/profile-1/reports', payload)
    expect(mockGet).toHaveBeenNthCalledWith(1, '/api/v1/posts/post-1/reports/me')
    expect(mockGet).toHaveBeenNthCalledWith(2, '/api/v1/comments/comment-1/reports/me')
    expect(mockGet).toHaveBeenNthCalledWith(3, '/api/v1/profiles/profile-1/reports/me')
  })

  it('maps known interaction and report failures to user-facing messages', () => {
    vi.spyOn(axios, 'isAxiosError').mockReturnValue(true)

    expect(interactionError({ response: { status: 401 } }, 'fallback'))
      .toBe('Inicia sesión para continuar.')
    expect(interactionError({
      response: { status: 400, data: { message: 'El contenido no es válido.' } },
    }, 'fallback')).toBe('El contenido no es válido.')
    expect(reportError({
      response: { status: 400, data: { code: 'OWN_CONTENT_REPORT' } },
    }, 'fallback')).toBe('No puedes reportar tu propio contenido.')
    expect(reportError({ response: { status: 409 } }, 'fallback'))
      .toBe('Ya reportaste este elemento y sigue pendiente de revisión.')
    expect(reportError({ response: { status: 401 } }, 'fallback'))
      .toBe('Inicia sesión para reportar.')
    expect(reportError(new Error('offline'), 'fallback')).toBe('fallback')
  })
})
