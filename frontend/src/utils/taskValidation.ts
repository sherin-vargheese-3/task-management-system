import type { TaskPayload } from '../types'

export const TITLE_MAX = 150
export const DESCRIPTION_MAX = 2000

export type TaskFormErrors = Partial<Record<keyof TaskPayload, string>>

export function validateTask(payload: TaskPayload): TaskFormErrors {
  const errors: TaskFormErrors = {}
  if (!payload.title.trim()) {
    errors.title = 'Title is required'
  } else if (payload.title.trim().length > TITLE_MAX) {
    errors.title = `Title must be at most ${TITLE_MAX} characters`
  }
  if (payload.description && payload.description.length > DESCRIPTION_MAX) {
    errors.description = `Description must be at most ${DESCRIPTION_MAX} characters`
  }
  if (payload.dueDate && Number.isNaN(Date.parse(payload.dueDate))) {
    errors.dueDate = 'Due date is invalid'
  }
  return errors
}
