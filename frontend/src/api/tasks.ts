import type { Page, Task, TaskPayload, TaskQuery, TaskSummary } from '../types'
import { buildQuery, request } from './client'

export const tasksApi = {
  list: (query: TaskQuery, signal?: AbortSignal) =>
    request<Page<Task>>(`/tasks${buildQuery({ ...query })}`, { signal }),
  summary: (signal?: AbortSignal) => request<TaskSummary>('/tasks/summary', { signal }),
  get: (id: number, signal?: AbortSignal) => request<Task>(`/tasks/${id}`, { signal }),
  create: (payload: TaskPayload) =>
    request<Task>('/tasks', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: number, payload: TaskPayload) =>
    request<Task>(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  assign: (id: number, assigneeId: number | null) =>
    request<Task>(`/tasks/${id}/assignee`, {
      method: 'PATCH',
      body: JSON.stringify({ assigneeId }),
    }),
  complete: (id: number) => request<Task>(`/tasks/${id}/complete`, { method: 'PATCH' }),
  remove: (id: number) => request<void>(`/tasks/${id}`, { method: 'DELETE' }),
}
