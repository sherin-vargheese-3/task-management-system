import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { EMPTY_FILTERS } from '../../hooks/useTaskQuery'
import { users } from '../../test/fixtures'
import { TaskFilters } from './TaskFilters'

describe('TaskFilters', () => {
  it('emits the updated filters when a status is selected', async () => {
    const onChange = vi.fn()
    render(<TaskFilters filters={EMPTY_FILTERS} users={users} onChange={onChange} />)

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'IN_PROGRESS')

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_FILTERS, status: 'IN_PROGRESS' })
  })

  it('offers an unassigned option and every user as assignee filters', () => {
    render(<TaskFilters filters={EMPTY_FILTERS} users={users} onChange={vi.fn()} />)

    const options = screen.getAllByRole('option', { name: /unassigned|alice|bob/i }).map((o) => o.textContent)

    expect(options).toEqual(['Unassigned', 'Alice Johnson', 'Bob Smith'])
  })

  it('clears every filter but keeps the sort order', async () => {
    const onChange = vi.fn()
    const active = { ...EMPTY_FILTERS, status: 'DONE' as const, search: 'docs', sort: 'dueDate,asc' }
    render(<TaskFilters filters={active} users={users} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button', { name: 'Clear filters' }))

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_FILTERS, sort: 'dueDate,asc' })
  })

  it('disables clear when no filter is active', () => {
    render(<TaskFilters filters={EMPTY_FILTERS} users={users} onChange={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Clear filters' })).toBeDisabled()
  })
})
