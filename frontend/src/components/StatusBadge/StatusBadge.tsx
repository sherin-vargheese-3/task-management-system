import type { TaskPriority, TaskStatus } from '../../types'
import { PRIORITY_LABELS, STATUS_LABELS } from '../../utils/format'

export function StatusBadge({ status }: { status: TaskStatus }) {
  return <span className={`badge badge-status-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
}

export function PriorityBadge({ priority }: { priority: TaskPriority }) {
  return (
    <span className={`badge badge-priority-${priority.toLowerCase()}`}>
      {PRIORITY_LABELS[priority]}
    </span>
  )
}
