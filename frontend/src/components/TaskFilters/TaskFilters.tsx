import type { ChangeEvent } from 'react'
import { EMPTY_FILTERS } from '../../hooks/useTaskQuery'
import type { TaskFilterValues } from '../../hooks/useTaskQuery'
import { TASK_PRIORITIES, TASK_STATUSES } from '../../types'
import type { User } from '../../types'
import { PRIORITY_LABELS, STATUS_LABELS } from '../../utils/format'

interface TaskFiltersProps {
  filters: TaskFilterValues
  users: User[]
  onChange: (filters: TaskFilterValues) => void
}

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'dueDate,asc', label: 'Due date' },
  { value: 'title,asc', label: 'Title A–Z' },
]

export function TaskFilters({ filters, users, onChange }: TaskFiltersProps) {
  const update =
    (key: keyof TaskFilterValues) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      onChange({ ...filters, [key]: event.target.value })

  const isFiltered =
    filters.status !== '' ||
    filters.priority !== '' ||
    filters.assignee !== '' ||
    filters.search !== ''

  return (
    <form className="filters" role="search" onSubmit={(event) => event.preventDefault()}>
      <label>
        Search
        <input
          type="search"
          value={filters.search}
          placeholder="Title or description"
          maxLength={100}
          onChange={update('search')}
        />
      </label>
      <label>
        Status
        <select value={filters.status} onChange={update('status')}>
          <option value="">All</option>
          {TASK_STATUSES.map((status) => (
            <option key={status} value={status}>
              {STATUS_LABELS[status]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Priority
        <select value={filters.priority} onChange={update('priority')}>
          <option value="">All</option>
          {TASK_PRIORITIES.map((priority) => (
            <option key={priority} value={priority}>
              {PRIORITY_LABELS[priority]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Assignee
        <select value={filters.assignee} onChange={update('assignee')}>
          <option value="">Anyone</option>
          <option value="none">Unassigned</option>
          {users.map((user) => (
            <option key={user.id} value={String(user.id)}>
              {user.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Sort
        <select value={filters.sort} onChange={update('sort')}>
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      <button
        type="button"
        className="button button-secondary"
        disabled={!isFiltered}
        onClick={() => onChange({ ...EMPTY_FILTERS, sort: filters.sort })}
      >
        Clear filters
      </button>
    </form>
  )
}
