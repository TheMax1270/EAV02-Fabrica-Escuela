import '@testing-library/jest-dom/vitest'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MessageForm from '../MessageForm'
import { sendMessage, type PrivateMessage } from '../../api/message'

vi.mock('../../api/message', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/message')>()),
  sendMessage: vi.fn(),
}))

const mockedSendMessage = vi.mocked(sendMessage)
const sent: PrivateMessage = {
  id: 'm1', conversationId: 'c1', senderId: 'me', recipientId: 'other', content: 'Hola', createdAt: '2026-11-10T12:00:00Z',
}

describe('MessageForm', () => {
  beforeEach(() => { mockedSendMessage.mockReset() })

  it('no permite enviar un mensaje vacío o solo con espacios', async () => {
    render(<MessageForm participantId="other" onSent={vi.fn()} />)
    const button = screen.getByRole('button', { name: /enviar/i })
    expect(button).toBeDisabled()

    await userEvent.type(screen.getByLabelText('Escribe un mensaje'), '   ')
    expect(button).toBeDisabled()
    expect(mockedSendMessage).not.toHaveBeenCalled()
  })

  it('envía el texto recortado con Enter y limpia el campo', async () => {
    mockedSendMessage.mockResolvedValue(sent)
    const onSent = vi.fn()
    render(<MessageForm participantId="other" onSent={onSent} />)
    const textarea = screen.getByLabelText('Escribe un mensaje')

    await userEvent.type(textarea, '  Hola  {Enter}')

    await waitFor(() => expect(onSent).toHaveBeenCalledWith(sent))
    expect(mockedSendMessage).toHaveBeenCalledWith('other', 'Hola')
    expect(textarea).toHaveValue('')
  })

  it('conserva el texto si el envío falla', async () => {
    mockedSendMessage.mockRejectedValue(new Error('network'))
    render(<MessageForm participantId="other" onSent={vi.fn()} />)
    const textarea = screen.getByLabelText('Escribe un mensaje')

    await userEvent.type(textarea, 'Hola')
    await userEvent.click(screen.getByRole('button', { name: /enviar/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Tu texto se conserva')
    expect(textarea).toHaveValue('Hola')
  })
})
