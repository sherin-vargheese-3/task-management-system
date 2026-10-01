import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { TASK_PRIORITIES, TASK_STATUSES } from '../types'
import type { TaskPriority, TaskQuery, TaskStatus } from '../types'

export const PAGE_SIZE = 10
export const DEFAULT_SORT = 'createdAt,desc'

export interface TaskFilterValues {
  status: TaskStatus | ''
  priority: TaskPriority | ''
  assignee: string
  search: string
  sort: string
}

export const EMPTY_FILTERS: TaskFilterValues = {
  status: '',
  priority: '',
  assignee: '',
  search: '',
  sort: DEFAULT_SORT,
}

function oneOf<T extends string>(value: string | null, allowed: readonly T[]): T | '' {
  return value !== null && (allowed as readonly string[]).includes(value) ? (value as T) : ''
}

export function useTaskQuery() {
  const [params, setParams] = useSearchParams()

  const filters: TaskFilterValues = useMemo(
    () => ({
      status: oneOf(params.get('status'), TASK_STATUSES),
      priority: oneOf(params.get('priority'), TASK_PRIORITIES),
      assignee: params.get('assignee') ?? '',
      search: params.get('search') ?? '',
      sort: params.get('sort') ?? DEFAULT_SORT,
    }),
    [params],
  )
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0)

  const setFilters = useCallback(
    (next: TaskFilterValues) => {
      const updated = new URLSearchParams()
      if (next.status) updated.set('status', next.status)
      if (next.priority) updated.set('priority', next.priority)
      if (next.assignee) updated.set('assignee', next.assignee)
      if (next.search) updated.set('search', next.search)
      if (next.sort !== DEFAULT_SORT) updated.set('sort', next.sort)
      setParams(updated, { replace: true })
    },
    [setParams],
  )

  const setPage = useCallback(
    (nextPage: number) => {
      setParams((previous) => {
        const updated = new URLSearchParams(previous)
        if (nextPage > 0) {
          updated.set('page', String(nextPage))
        } else {
          updated.delete('page')
        }
        return updated
      })
    },
    [setParams],
  )

  return { filters, page, setFilters, setPage }
}

export function toTaskQuery(filters: TaskFilterValues, page: number): TaskQuery {
  const unassigned = filters.assignee === 'none'
  const assigneeId = !unassigned && filters.assignee ? Number(filters.assignee) : undefined
  return {
    status: filters.status || undefined,
    priority: filters.priority || undefined,
    assigneeId,
    unassigned: unassigned || undefined,
    search: filters.search.trim() || undefined,
    page,
    size: PAGE_SIZE,
    sort: filters.sort,
  }
}
