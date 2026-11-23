import '@testing-library/jest-dom/vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminStatisticsPage from '../AdminStatisticsPage'
import { getStatistics, type StatisticsResponse } from '../../api/statistics'

vi.mock('../../api/statistics', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/statistics')>()),
  getStatistics: vi.fn(),
}))

vi.mock('../../components/SiteHeader', () => ({
  default: () => <header>DevConnect</header>,
}))

const mockedGetStatistics = vi.mocked(getStatistics)

const response: StatisticsResponse = {
  startDate: '2026-11-01',
  endDate: '2026-11-30',
  groupBy: 'MONTH',
  totals: { users: 4, projects: 2, posts: 3, comments: 5 },
  series: [
    { period: '2026-11-01', users: 4, projects: 2, posts: 3, comments: 5 },
    { period: '2026-11-08', users: 0, projects: 0, posts: 0, comments: 0 },
  ],
}

const emptyResponse: StatisticsResponse = {
  ...response,
  totals: { users: 0, projects: 0, posts: 0, comments: 0 },
  series: [{ period: '2026-11-01', users: 0, projects: 0, posts: 0, comments: 0 }],
}

function renderPage() {
  return render(<AdminStatisticsPage />)
}

async function waitForInitialResults() {
  await screen.findByText(/Resultados del/)
}

describe('AdminStatisticsPage', () => {
  beforeEach(() => {
    mockedGetStatistics.mockReset().mockResolvedValue(response)
  })

  it('muestra los cuatro indicadores recibidos por la API', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Estadísticas de la plataforma' })).toBeInTheDocument()
    await waitForInitialResults()
    expect(screen.getByRole('heading', { name: 'Usuarios registrados' }).parentElement).toHaveTextContent('4')
    expect(screen.getByRole('heading', { name: 'Proyectos creados' }).parentElement).toHaveTextContent('2')
    expect(screen.getByRole('heading', { name: 'Publicaciones y discusiones' }).parentElement).toHaveTextContent('3')
    expect(screen.getByRole('heading', { name: 'Comentarios realizados' }).parentElement).toHaveTextContent('5')
  })

  it('envía las fechas seleccionadas y la agrupación semanal', async () => {
    renderPage()
    await waitForInitialResults()
    mockedGetStatistics.mockClear()

    fireEvent.change(screen.getByLabelText('Fecha inicial'), { target: { value: '2026-10-01' } })
    fireEvent.change(screen.getByLabelText('Fecha final'), { target: { value: '2026-11-10' } })
    fireEvent.change(screen.getByLabelText('Agrupación'), { target: { value: 'WEEK' } })
    fireEvent.click(screen.getByRole('button', { name: 'Consultar estadísticas' }))

    await waitFor(() => {
      expect(mockedGetStatistics).toHaveBeenCalledWith({
        startDate: '2026-10-01',
        endDate: '2026-11-10',
        groupBy: 'WEEK',
      })
    })
  })

  it('permite cambiar la métrica representada en la gráfica', async () => {
    renderPage()
    await waitForInitialResults()

    fireEvent.change(screen.getByLabelText('Métrica de la gráfica'), { target: { value: 'projects' } })

    expect(screen.getByRole('figure', { name: 'Gráfica de barras de proyectos por período' })).toBeInTheDocument()
  })

  it('muestra períodos con cero y un mensaje cuando no hay registros', async () => {
    mockedGetStatistics.mockResolvedValue(emptyResponse)
    renderPage()

    expect(await screen.findByText('No hay registros para el período consultado.')).toBeInTheDocument()
    expect(screen.getByRole('row', { name: /2026-11-01/ })).toHaveTextContent('0')
    expect(screen.getByRole('figure', { name: 'Gráfica de barras de usuarios por período' })).toBeInTheDocument()
  })

  it('rechaza fechas invertidas y rangos mayores de cinco años sin llamar a la API', async () => {
    renderPage()
    await waitForInitialResults()
    mockedGetStatistics.mockClear()

    fireEvent.change(screen.getByLabelText('Fecha inicial'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Consultar estadísticas' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Selecciona una fecha inicial y una fecha final.')
    expect(mockedGetStatistics).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText('Fecha inicial'), { target: { value: '2026-11-10' } })
    fireEvent.change(screen.getByLabelText('Fecha final'), { target: { value: '2026-11-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Consultar estadísticas' }))
    expect(screen.getByRole('alert')).toHaveTextContent('La fecha inicial no puede ser posterior a la fecha final.')
    expect(mockedGetStatistics).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText('Fecha inicial'), { target: { value: '2020-01-01' } })
    fireEvent.change(screen.getByLabelText('Fecha final'), { target: { value: '2025-01-02' } })
    fireEvent.click(screen.getByRole('button', { name: 'Consultar estadísticas' }))
    expect(screen.getByRole('alert')).toHaveTextContent('El rango no puede superar cinco años.')
    expect(mockedGetStatistics).not.toHaveBeenCalled()
  })

  it('acepta exactamente cinco años calendario', async () => {
    renderPage()
    await waitForInitialResults()
    mockedGetStatistics.mockClear()

    fireEvent.change(screen.getByLabelText('Fecha inicial'), { target: { value: '2021-01-01' } })
    fireEvent.change(screen.getByLabelText('Fecha final'), { target: { value: '2026-01-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Consultar estadísticas' }))

    await waitFor(() => {
      expect(mockedGetStatistics).toHaveBeenCalledWith({
        startDate: '2021-01-01',
        endDate: '2026-01-01',
        groupBy: 'MONTH',
      })
    })
  })

  it('muestra el estado de carga y permite reintentar un error HTTP 400', async () => {
    mockedGetStatistics
      .mockRejectedValueOnce({
        isAxiosError: true,
        response: { status: 400, data: { errors: { startDate: 'Fecha no válida.' } } },
      })
      .mockResolvedValueOnce(response)
    renderPage()

    expect(screen.getByRole('status')).toHaveTextContent('Cargando estadísticas...')
    expect(await screen.findByRole('alert')).toHaveTextContent('El servidor rechazó la consulta: Fecha no válida.')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    await waitFor(() => expect(mockedGetStatistics).toHaveBeenCalledTimes(2))
    await waitForInitialResults()
  })

  it('muestra un mensaje de conexión cuando la petición falla sin respuesta HTTP', async () => {
    mockedGetStatistics.mockRejectedValueOnce(new Error('network unavailable'))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos conectar con el servicio de estadísticas.',
    )
  })

  it('muestra mensajes distintos para respuestas no autenticadas y no autorizadas', async () => {
    mockedGetStatistics
      .mockRejectedValueOnce({ isAxiosError: true, response: { status: 401 } })
      .mockRejectedValueOnce({ isAxiosError: true, response: { status: 403 } })
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Tu sesión ya no está autorizada.')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permisos para consultar estas estadísticas.')
  })

  it('ignora la respuesta de una consulta anterior si llega después de la nueva', async () => {
    let resolveFirst!: (value: StatisticsResponse) => void
    const firstRequest = new Promise<StatisticsResponse>((resolve) => { resolveFirst = resolve })
    const outdatedResponse: StatisticsResponse = {
      ...response,
      totals: { users: 99, projects: 0, posts: 0, comments: 0 },
    }
    mockedGetStatistics.mockReturnValueOnce(firstRequest).mockResolvedValueOnce(response)
    renderPage()

    fireEvent.change(screen.getByLabelText('Fecha inicial'), { target: { value: '2026-10-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Consultar estadísticas' }))
    await waitForInitialResults()

    await act(async () => { resolveFirst(outdatedResponse) })
    expect(screen.getByRole('heading', { name: 'Usuarios registrados' }).parentElement).toHaveTextContent('4')
    expect(screen.getByRole('heading', { name: 'Usuarios registrados' }).parentElement).not.toHaveTextContent('99')
  })
})
