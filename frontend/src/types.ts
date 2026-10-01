export const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'] as const
export const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const

export type TaskStatus = (typeof TASK_STATUSES)[number]
export type TaskPriority = (typeof TASK_PRIORITIES)[number]

export interface Assignee {
  id: number
  name: string
}

export interface Task {
  id: number
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  assignee: Assignee | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface User {
  id: number
  name: string
  email: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface TaskSummary {
  total: number
  todo: number
  inProgress: number
  done: number
}

export interface TaskPayload {
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  assigneeId: number | null
}

export interface TaskQuery {
  status?: TaskStatus
  priority?: TaskPriority
  assigneeId?: number
  unassigned?: boolean
  search?: string
  page: number
  size: number
  sort: string
}
