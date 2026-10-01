import type { Page, Task, TaskSummary, User } from '../types'

export const users: User[] = [
  { id: 1, name: 'Alice Johnson', email: 'alice@example.com' },
  { id: 2, name: 'Bob Smith', email: 'bob@example.com' },
]

export function makeTask(overrides: Partial<Task> = {}): Task {
  return {
    id: 1,
    title: 'Write docs',
    description: 'Document the API',
    status: 'TODO',
    priority: 'HIGH',
    dueDate: '2026-10-10',
    assignee: { id: 1, name: 'Alice Johnson' },
    completedAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    ...overrides,
  }
}

export function pageOf<T>(content: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
    ...overrides,
  }
}

export const summary: TaskSummary = { total: 3, todo: 1, inProgress: 1, done: 1 }
