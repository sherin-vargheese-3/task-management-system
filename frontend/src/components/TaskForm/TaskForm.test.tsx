import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { makeTask, users } from '../../test/fixtures'
import { TaskForm } from './TaskForm'

describe('TaskForm', () => {
  it('shows a validation error and does not submit when title is empty', async () => {
    const onSubmit = vi.fn()
    render(<TaskForm users={users} submitLabel="Create task" onSubmit={onSubmit} />)

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(screen.getByText('Title is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('rejects a title longer than 150 characters', async () => {
    const onSubmit = vi.fn()
    render(<TaskForm users={users} submitLabel="Create task" onSubmit={onSubmit} />)

    await userEvent.click(screen.getByLabelText(/title/i))
    await userEvent.paste('a'.repeat(151))
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(screen.getByText('Title must be at most 150 characters')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits a trimmed payload with the selected assignee', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(<TaskForm users={users} submitLabel="Create task" onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText(/title/i), '  Ship release  ')
    await userEvent.selectOptions(screen.getByLabelText('Priority'), 'HIGH')
    await userEvent.selectOptions(screen.getByLabelText('Assignee'), '2')
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Ship release',
      description: null,
      status: 'TODO',
      priority: 'HIGH',
      dueDate: null,
      assigneeId: 2,
    })
  })

  it('prefills values when editing an existing task', () => {
    render(<TaskForm initialTask={makeTask()} users={users} submitLabel="Save" onSubmit={vi.fn()} />)

    expect(screen.getByLabelText(/title/i)).toHaveValue('Write docs')
    expect(screen.getByLabelText('Assignee')).toHaveValue('1')
    expect(screen.getByLabelText('Priority')).toHaveValue('HIGH')
  })

  it('shows server-side field errors returned by the API', async () => {
    const onSubmit = vi
      .fn()
      .mockRejectedValue(
        new ApiError(400, 'Validation failed', [{ field: 'title', message: 'must not be blank' }]),
      )
    render(<TaskForm users={users} submitLabel="Create task" onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText(/title/i), 'x')
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Validation failed')
    expect(screen.getByText('must not be blank')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create task' })).toBeEnabled()
  })
})
