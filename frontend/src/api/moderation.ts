import { api } from './client'

export type ReportTargetType = 'POST' | 'COMMENT' | 'PROFILE'
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED'
export type ModerationStatus = 'VISIBLE' | 'IN_REVIEW' | 'REMOVED'
export type ReportReason = 'SPAM' | 'HARASSMENT' | 'INAPPROPRIATE_CONTENT' | 'MISINFORMATION' | 'OTHER'

export type AdminReport = {
  id: string
  reporterId: string
  reporterUsername: string
  targetType: ReportTargetType
  targetId: string
  targetPreview: string
  targetUsername: string | null
  reason: ReportReason
  details: string | null
  status: ReportStatus
  moderationStatus: ModerationStatus | null
  createdAt: string
}

export type AdminReportPage = {
  content: AdminReport[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listPendingReports(page: number, size: number): Promise<AdminReportPage> {
  const response = await api.get<AdminReportPage>('/api/v1/admin/reports', { params: { page, size } })
  return response.data
}

async function reportAction(reportId: string, action: 'review' | 'restore' | 'remove' | 'dismiss') {
  const response = await api.patch<AdminReport>(`/api/v1/admin/reports/${reportId}/${action}`)
  return response.data
}

export const reviewReport = (reportId: string) => reportAction(reportId, 'review')
export const restoreReport = (reportId: string) => reportAction(reportId, 'restore')
export const removeReportedContent = (reportId: string) => reportAction(reportId, 'remove')
export const dismissReport = (reportId: string) => reportAction(reportId, 'dismiss')
