import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { tasksApi } from '../api/tasks'
import { usersApi } from '../api/users'
import { makeTask, pageOf, users } from '../test/fixtures'
import { TaskDetailsPage } from './TaskDetailsPage'

vi.mock('../api/tasks')
vi.mock('../api/users')

function renderDetails(path = '/tasks/1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<h1>Dashboard home</h1>} />
        <Route path="/tasks/:id" element={<TaskDetailsPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('TaskDetailsPage', () => {
  beforeEach(() => {
    vi.mocked(usersApi.list).mockResolvedValue(pageOf(users))
    vi.mocked(tasksApi.get).mockResolvedValue(makeTask())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the task details after loading', async () => {
    renderDetails()

    expect(screen.getByText('Loading task…')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Write docs' })).toBeInTheDocument()
    expect(screen.getByText('Document the API')).toBeInTheDocument()
    expect(screen.getByLabelText('Assignee')).toHaveValue('1')
  })

  it('shows a not found error from the API', async () => {
    vi.mocked(tasksApi.get).mockRejectedValue(new ApiError(404, 'Task with id 1 not found'))

    renderDetails()

    expect(await screen.findByText('Task with id 1 not found')).toBeInTheDocument()
  })

  it('rejects an invalid id without calling the API', () => {
    renderDetails('/tasks/abc')

    expect(screen.getByText('Invalid task id')).toBeInTheDocument()
    expect(tasksApi.get).not.toHaveBeenCalled()
  })

  it('marks the task complete and shows the new status', async () => {
    vi.mocked(tasksApi.complete).mockResolvedValue(
      makeTask({ status: 'DONE', completedAt: '2026-10-02T10:00:00Z' }),
    )
    renderDetails()
    await screen.findByRole('heading', { name: 'Write docs' })

    await userEvent.click(screen.getByRole('button', { name: 'Mark complete' }))

    expect(tasksApi.complete).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Done')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mark complete' })).not.toBeInTheDocument()
  })

  it('reassigns the task through the assignee selector', async () => {
    vi.mocked(tasksApi.assign).mockResolvedValue(makeTask({ assignee: { id: 2, name: 'Bob Smith' } }))
    renderDetails()
    await screen.findByRole('heading', { name: 'Write docs' })

    await userEvent.selectOptions(screen.getByLabelText('Assignee'), '2')

    expect(tasksApi.assign).toHaveBeenCalledWith(1, 2)
    expect(await screen.findByLabelText('Assignee')).toHaveValue('2')
  })

  it('unassigns the task', async () => {
    vi.mocked(tasksApi.assign).mockResolvedValue(makeTask({ assignee: null }))
    renderDetails()
    await screen.findByRole('heading', { name: 'Write docs' })

    await userEvent.selectOptions(screen.getByLabelText('Assignee'), '')

    expect(tasksApi.assign).toHaveBeenCalledWith(1, null)
  })

  it('edits the task and shows the updated values', async () => {
    vi.mocked(tasksApi.update).mockResolvedValue(makeTask({ title: 'Write better docs', status: 'IN_PROGRESS' }))
    renderDetails()
    await screen.findByRole('heading', { name: 'Write docs' })

    await userEvent.click(screen.getByRole('button', { name: 'Edit' }))
    const title = screen.getByLabelText(/title/i)
    await userEvent.clear(title)
    await userEvent.type(title, 'Write better docs')
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'IN_PROGRESS')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(tasksApi.update).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ title: 'Write better docs', status: 'IN_PROGRESS', assigneeId: 1 }),
    )
    expect(await screen.findByRole('heading', { name: 'Write better docs' })).toBeInTheDocument()
  })

  it('deletes the task after confirmation and returns to the dashboard', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(tasksApi.remove).mockResolvedValue(undefined)
    renderDetails()
    await screen.findByRole('heading', { name: 'Write docs' })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(tasksApi.remove).toHaveBeenCalledWith(1)
    expect(await screen.findByRole('heading', { name: 'Dashboard home' })).toBeInTheDocument()
  })

  it('does not delete when the confirmation is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderDetails()
    await screen.findByRole('heading', { name: 'Write docs' })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(tasksApi.remove).not.toHaveBeenCalled()
  })

  it('tells the user when the assignee list cannot be loaded', async () => {
    vi.mocked(usersApi.list).mockRejectedValue(new ApiError(0, 'Unable to reach the server'))

    renderDetails()

    expect(await screen.findByText('Could not load users: Unable to reach the server')).toBeInTheDocument()
  })
})
