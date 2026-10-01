import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { tasksApi } from '../api/tasks'
import { EmptyState, ErrorMessage, Loading } from '../components/Feedback/Feedback'
import { Pagination } from '../components/Pagination/Pagination'
import { SummaryCards } from '../components/SummaryCards/SummaryCards'
import { TaskFilters } from '../components/TaskFilters/TaskFilters'
import { TaskTable } from '../components/TaskTable/TaskTable'
import { useApi } from '../hooks/useApi'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { toTaskQuery, useTaskQuery } from '../hooks/useTaskQuery'
import { useUsers } from '../hooks/useUsers'
import type { Task } from '../types'

export const SEARCH_DEBOUNCE_MS = 300

export function DashboardPage() {
  const { filters, page, setFilters, setPage } = useTaskQuery()
  const debouncedSearch = useDebouncedValue(filters.search, SEARCH_DEBOUNCE_MS)
  const { users } = useUsers()
  const [busyTaskId, setBusyTaskId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { status, priority, assignee, sort } = filters
  const query = useMemo(
    () => toTaskQuery({ status, priority, assignee, sort, search: debouncedSearch }, page),
    [status, priority, assignee, sort, debouncedSearch, page],
  )
  const fetchTasks = useCallback((signal: AbortSignal) => tasksApi.list(query, signal), [query])
  const fetchSummary = useCallback((signal: AbortSignal) => tasksApi.summary(signal), [])
  const tasks = useApi(fetchTasks)
  const summary = useApi(fetchSummary)

  const totalPages = tasks.data?.totalPages ?? 0
  useEffect(() => {
    if (tasks.data && page > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [tasks.data, page, totalPages, setPage])

  const reloadTasks = tasks.reload
  const reloadSummary = summary.reload
  const handleComplete = useCallback(
    async (task: Task) => {
      setBusyTaskId(task.id)
      setActionError(null)
      try {
        await tasksApi.complete(task.id)
      } catch (error) {
        setActionError(error instanceof Error ? error.message : 'Could not complete the task')
      } finally {
        setBusyTaskId(null)
        reloadTasks()
        reloadSummary()
      }
    },
    [reloadTasks, reloadSummary],
  )

  return (
    <div className="page">
      <header className="page-header">
        <h1>Dashboard</h1>
        <Link to="/tasks/new" className="button">
          New task
        </Link>
      </header>

      {summary.error ? (
        <ErrorMessage error={summary.error} onRetry={summary.reload} />
      ) : summary.data ? (
        <SummaryCards summary={summary.data} />
      ) : (
        <Loading label="Loading summary…" />
      )}

      <TaskFilters filters={filters} users={users} onChange={setFilters} />

      {actionError && <ErrorMessage error={actionError} />}

      {tasks.error ? (
        <ErrorMessage error={tasks.error} onRetry={tasks.reload} />
      ) : tasks.loading && !tasks.data ? (
        <Loading label="Loading tasks…" />
      ) : tasks.data && tasks.data.content.length === 0 ? (
        <EmptyState message="No tasks match the current filters." />
      ) : tasks.data ? (
        <div className={tasks.loading ? 'is-refreshing' : undefined} aria-busy={tasks.loading}>
          <TaskTable tasks={tasks.data.content} busyTaskId={busyTaskId} onComplete={handleComplete} />
          <Pagination
            page={tasks.data.page}
            totalPages={tasks.data.totalPages}
            totalElements={tasks.data.totalElements}
            onPageChange={setPage}
          />
        </div>
      ) : null}
    </div>
  )
}
