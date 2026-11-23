import '@testing-library/jest-dom/vitest'
import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import RepliesPanel from '../RepliesPanel'

vi.mock('../CommentThread', () => ({
  default: ({ path, replies, onCreated, onDeleted }: {
    path: string; replies: boolean; onCreated: () => void; onDeleted: () => void
  }) => <div data-testid="reply-thread" data-replies={replies}>
    {path}<button onClick={onCreated}>Created callback</button><button onClick={onDeleted}>Deleted callback</button>
  </div>,
}))

describe('RepliesPanel', () => {
  it('provides the comment reply endpoint and forwards lifecycle callbacks', () => {
    const onCreated = vi.fn()
    const onDeleted = vi.fn()
    render(<RepliesPanel commentId="comment-3" onCreated={onCreated} onDeleted={onDeleted} />)

    expect(screen.getByTestId('reply-thread')).toHaveTextContent('/api/v1/comments/comment-3/replies')
    expect(screen.getByTestId('reply-thread')).toHaveAttribute('data-replies', 'true')
    screen.getByRole('button', { name: 'Created callback' }).click()
    screen.getByRole('button', { name: 'Deleted callback' }).click()
    expect(onCreated).toHaveBeenCalledOnce()
    expect(onDeleted).toHaveBeenCalledOnce()
  })
})
