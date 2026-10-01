import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { tasksApi } from '../api/tasks'
import { ErrorMessage, Loading } from '../components/Feedback/Feedback'
import { PriorityBadge, StatusBadge } from '../components/StatusBadge/StatusBadge'
import { TaskForm } from '../components/TaskForm/TaskForm'
import { useApi } from '../hooks/useApi'
import { useUsers } from '../hooks/useUsers'
import type { Task, TaskPayload } from '../types'
import { formatDate, formatDateTime } from '../utils/format'

type PendingAction = 'complete' | 'assign' | 'delete' | null

export function TaskDetailsPage() {
  const { id } = useParams()
  const taskId = Number(id)
  const validId = Number.isInteger(taskId) && taskId > 0

  if (!validId) {
    return (
      <div className="page page-narrow">
        <ErrorMessage error="Invalid task id" />
        <Link to="/">Back to dashboard</Link>
      </div>
    )
  }
  return <TaskDetails key={taskId} taskId={taskId} />
}

function TaskDetails({ taskId }: { taskId: number }) {
  const navigate = useNavigate()
  const { users } = useUsers()
  const fetchTask = useCallback((signal: AbortSignal) => tasksApi.get(taskId, signal), [taskId])
  const { data: task, loading, error, reload, setData } = useApi(fetchTask)
  const [editing, setEditing] = useState(false)
  const [pending, setPending] = useState<PendingAction>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const runAction = async (action: Exclude<PendingAction, null>, operation: () => Promise<Task | void>) => {
    setPending(action)
    setActionError(null)
    try {
      const updated = await operation()
      if (updated) {
        setData(updated)
      }
      return true
    } catch (actionFailure) {
      setActionError(actionFailure instanceof Error ? actionFailure.message : 'Action failed')
      return false
    } finally {
      setPending(null)
    }
  }

  if (loading && !task) {
    return <Loading label="Loading task…" />
  }
  if (error || !task) {
    return (
      <div className="page page-narrow">
        <ErrorMessage error={error ?? 'Task not found'} onRetry={reload} />
        <Link to="/">Back to dashboard</Link>
      </div>
    )
  }

  const handleUpdate = async (payload: TaskPayload) => {
    const updated = await tasksApi.update(task.id, payload)
    setData(updated)
    setEditing(false)
  }

  const handleAssign = (value: string) =>
    runAction('assign', () => tasksApi.assign(task.id, value ? Number(value) : null))

  const handleComplete = () => runAction('complete', () => tasksApi.complete(task.id))

  const handleDelete = async () => {
    if (!window.confirm(`Delete "${task.title}"? This cannot be undone.`)) {
      return
    }
    const deleted = await runAction('delete', () => tasksApi.remove(task.id))
    if (deleted) {
      navigate('/', { replace: true })
    }
  }

  return (
    <div className="page page-narrow">
      <p>
        <Link to="/">← Back to dashboard</Link>
      </p>
      {editing ? (
        <>
          <header className="page-header">
            <h1>Edit task</h1>
          </header>
          <TaskForm
            initialTask={task}
            users={users}
            submitLabel="Save changes"
            onSubmit={handleUpdate}
            onCancel={() => setEditing(false)}
          />
        </>
      ) : (
        <article className="task-details">
          <header className="page-header">
            <h1>{task.title}</h1>
            <div className="badges">
              <StatusBadge status={task.status} />
              <PriorityBadge priority={task.priority} />
            </div>
          </header>

          {actionError && <ErrorMessage error={actionError} />}

          <p className="description">{task.description || <span className="muted">No description</span>}</p>

          <dl className="details-grid">
            <dt>Due date</dt>
            <dd>{formatDate(task.dueDate)}</dd>
            <dt>Created</dt>
            <dd>{formatDateTime(task.createdAt)}</dd>
            <dt>Last updated</dt>
            <dd>{formatDateTime(task.updatedAt)}</dd>
            <dt>Completed</dt>
            <dd>{formatDateTime(task.completedAt)}</dd>
            <dt>
              <label htmlFor="assignee-select">Assignee</label>
            </dt>
            <dd>
              <select
                id="assignee-select"
                value={task.assignee ? String(task.assignee.id) : ''}
                disabled={pending !== null}
                onChange={(event) => handleAssign(event.target.value)}
              >
                <option value="">Unassigned</option>
                {task.assignee && !users.some((user) => user.id === task.assignee?.id) && (
                  <option value={String(task.assignee.id)}>{task.assignee.name}</option>
                )}
                {users.map((user) => (
                  <option key={user.id} value={String(user.id)}>
                    {user.name}
                  </option>
                ))}
              </select>
            </dd>
          </dl>

          <div className="form-actions">
            {task.status !== 'DONE' && (
              <button type="button" className="button" disabled={pending !== null} onClick={handleComplete}>
                {pending === 'complete' ? 'Completing…' : 'Mark complete'}
              </button>
            )}
            <button
              type="button"
              className="button button-secondary"
              disabled={pending !== null}
              onClick={() => setEditing(true)}
            >
              Edit
            </button>
            <button
              type="button"
              className="button button-danger"
              disabled={pending !== null}
              onClick={handleDelete}
            >
              {pending === 'delete' ? 'Deleting…' : 'Delete'}
            </button>
          </div>
        </article>
      )}
    </div>
  )
}
