import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { TASK_PRIORITIES, TASK_STATUSES } from '../../types'
import type { Task, TaskPayload, User } from '../../types'
import { PRIORITY_LABELS, STATUS_LABELS } from '../../utils/format'
import { DESCRIPTION_MAX, validateTask } from '../../utils/taskValidation'
import type { TaskFormErrors } from '../../utils/taskValidation'

interface TaskFormProps {
  initialTask?: Task
  users: User[]
  submitLabel: string
  onSubmit: (payload: TaskPayload) => Promise<void>
  onCancel?: () => void
}

interface FormValues {
  title: string
  description: string
  status: TaskPayload['status']
  priority: TaskPayload['priority']
  dueDate: string
  assigneeId: string
}

function toFormValues(task?: Task): FormValues {
  return {
    title: task?.title ?? '',
    description: task?.description ?? '',
    status: task?.status ?? 'TODO',
    priority: task?.priority ?? 'MEDIUM',
    dueDate: task?.dueDate ?? '',
    assigneeId: task?.assignee ? String(task.assignee.id) : '',
  }
}

function toPayload(values: FormValues): TaskPayload {
  return {
    title: values.title.trim(),
    description: values.description.trim() || null,
    status: values.status,
    priority: values.priority,
    dueDate: values.dueDate || null,
    assigneeId: values.assigneeId ? Number(values.assigneeId) : null,
  }
}

function serverFieldErrors(error: ApiError): TaskFormErrors {
  const errors: TaskFormErrors = {}
  error.fieldErrors.forEach(({ field, message }) => {
    if (field in toFormValues()) {
      errors[field as keyof TaskFormErrors] = message
    }
  })
  return errors
}

export function TaskForm({ initialTask, users, submitLabel, onSubmit, onCancel }: TaskFormProps) {
  const [values, setValues] = useState<FormValues>(() => toFormValues(initialTask))
  const [errors, setErrors] = useState<TaskFormErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const update =
    (key: keyof FormValues) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
      setValues((previous) => ({ ...previous, [key]: event.target.value }))
      setErrors((previous) => ({ ...previous, [key]: undefined }))
    }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const payload = toPayload(values)
    const validationErrors = validateTask(payload)
    setErrors(validationErrors)
    setFormError(null)
    if (Object.keys(validationErrors).length > 0) {
      return
    }
    setSubmitting(true)
    try {
      await onSubmit(payload)
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors(serverFieldErrors(error))
        setFormError(error.message)
      } else {
        setFormError('Something went wrong while saving the task')
      }
      setSubmitting(false)
    }
  }

  return (
    <form className="task-form" onSubmit={handleSubmit} noValidate>
      {formError && (
        <div className="feedback feedback-error" role="alert">
          {formError}
        </div>
      )}
      <label>
        Title *
        <input
          value={values.title}
          onChange={update('title')}
          aria-invalid={Boolean(errors.title)}
          aria-describedby={errors.title ? 'title-error' : undefined}
        />
        {errors.title && (
          <span id="title-error" className="field-error">
            {errors.title}
          </span>
        )}
      </label>
      <label>
        Description
        <textarea
          value={values.description}
          onChange={update('description')}
          rows={4}
          aria-invalid={Boolean(errors.description)}
        />
        {errors.description && <span className="field-error">{errors.description}</span>}
        <span className="hint">
          {values.description.length}/{DESCRIPTION_MAX}
        </span>
      </label>
      <div className="form-row">
        <label>
          Status
          <select value={values.status} onChange={update('status')}>
            {TASK_STATUSES.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </label>
        <label>
          Priority
          <select value={values.priority} onChange={update('priority')}>
            {TASK_PRIORITIES.map((priority) => (
              <option key={priority} value={priority}>
                {PRIORITY_LABELS[priority]}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="form-row">
        <label>
          Due date
          <input type="date" value={values.dueDate} onChange={update('dueDate')} />
          {errors.dueDate && <span className="field-error">{errors.dueDate}</span>}
        </label>
        <label>
          Assignee
          <select value={values.assigneeId} onChange={update('assigneeId')}>
            <option value="">Unassigned</option>
            {users.map((user) => (
              <option key={user.id} value={String(user.id)}>
                {user.name}
              </option>
            ))}
          </select>
          {errors.assigneeId && <span className="field-error">{errors.assigneeId}</span>}
        </label>
      </div>
      <div className="form-actions">
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" className="button button-secondary" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
