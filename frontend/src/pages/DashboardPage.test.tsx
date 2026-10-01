import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { tasksApi } from '../api/tasks'
import { usersApi } from '../api/users'
import { makeTask, pageOf, summary, users } from '../test/fixtures'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/tasks')
vi.mock('../api/users')

function renderDashboard(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(usersApi.list).mockResolvedValue(pageOf(users))
    vi.mocked(tasksApi.summary).mockResolvedValue(summary)
    vi.mocked(tasksApi.list).mockResolvedValue(pageOf([makeTask()]))
  })

  it('shows a loading state and then the summary and task list', async () => {
    renderDashboard()

    expect(screen.getByText('Loading tasks…')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Write docs' })).toHaveAttribute('href', '/tasks/1')
    const summarySection = screen.getByRole('region', { name: 'Task summary' })
    expect(within(summarySection).getByText('Total').previousSibling).toHaveTextContent('3')
  })

  it('shows an empty state when no tasks match', async () => {
    vi.mocked(tasksApi.list).mockResolvedValue(pageOf([]))

    renderDashboard()

    expect(await screen.findByText('No tasks match the current filters.')).toBeInTheDocument()
  })

  it('shows an error with retry when tasks fail to load', async () => {
    vi.mocked(tasksApi.list)
      .mockRejectedValueOnce(new ApiError(0, 'Unable to reach the server'))
      .mockResolvedValueOnce(pageOf([makeTask()]))

    renderDashboard()

    expect(await screen.findByText('Unable to reach the server')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('link', { name: 'Write docs' })).toBeInTheDocument()
  })

  it('requests tasks with the selected filters and resets to the first page', async () => {
    renderDashboard('/?page=2')
    await screen.findByRole('link', { name: 'Write docs' })

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'DONE')
    await userEvent.selectOptions(screen.getByLabelText('Assignee'), 'none')

    expect(tasksApi.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'DONE', unassigned: true, assigneeId: undefined, page: 0 }),
      expect.any(AbortSignal),
    )
  })

  it('applies initial filters from the URL', async () => {
    renderDashboard('/?priority=HIGH&assignee=2&page=1')

    await screen.findByRole('link', { name: 'Write docs' })

    expect(tasksApi.list).toHaveBeenCalledWith(
      expect.objectContaining({ priority: 'HIGH', assigneeId: 2, page: 1 }),
      expect.any(AbortSignal),
    )
  })

  it('debounces search input before querying', async () => {
    renderDashboard()
    await screen.findByRole('link', { name: 'Write docs' })
    vi.mocked(tasksApi.list).mockClear()

    await userEvent.type(screen.getByLabelText('Search'), 'docs')

    await vi.waitFor(() =>
      expect(tasksApi.list).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'docs' }),
        expect.any(AbortSignal),
      ),
    )
    expect(tasksApi.list).toHaveBeenCalledTimes(1)
  })

  it('completes a task and refreshes the list and summary', async () => {
    vi.mocked(tasksApi.complete).mockResolvedValue(makeTask({ status: 'DONE' }))
    renderDashboard()
    await screen.findByRole('link', { name: 'Write docs' })
    vi.mocked(tasksApi.list).mockResolvedValue(pageOf([makeTask({ status: 'DONE' })]))
    vi.mocked(tasksApi.summary).mockResolvedValue({ total: 3, todo: 0, inProgress: 1, done: 2 })

    await userEvent.click(screen.getByRole('button', { name: 'Complete Write docs' }))

    expect(tasksApi.complete).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Done', { selector: '.badge' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Complete Write docs' })).not.toBeInTheDocument()
    const summarySection = screen.getByRole('region', { name: 'Task summary' })
    expect(within(summarySection).getByText('Done').previousSibling).toHaveTextContent('2')
  })

  it('shows the error when completing a task fails', async () => {
    vi.mocked(tasksApi.complete).mockRejectedValue(new ApiError(409, 'Task 1 is already completed'))
    renderDashboard()
    await screen.findByRole('link', { name: 'Write docs' })

    await userEvent.click(screen.getByRole('button', { name: 'Complete Write docs' }))

    expect(await screen.findByText('Task 1 is already completed')).toBeInTheDocument()
  })
})
