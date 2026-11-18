import '@testing-library/jest-dom/vitest'
import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import CommentsPanel from '../CommentsPanel'

vi.mock('../CommentThread', () => ({
  default: ({ path }: { path: string }) => <div data-testid="comment-thread">{path}</div>,
}))

describe('CommentsPanel', () => {
  it('renders the comments section for the selected target', () => {
    render(<CommentsPanel target={{ kind: 'projects', id: 'project-7' }} />)

    expect(screen.getByRole('region', { name: 'Comentarios' })).toBeInTheDocument()
    expect(screen.getByTestId('comment-thread')).toHaveTextContent('/api/v1/projects/project-7/comments')
  })
})
