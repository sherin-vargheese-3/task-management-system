import { Link } from 'react-router-dom'
import type { Task } from '../../types'
import { formatDate } from '../../utils/format'
import { PriorityBadge, StatusBadge } from '../StatusBadge/StatusBadge'

interface TaskTableProps {
  tasks: Task[]
  busyTaskId: number | null
  onComplete: (task: Task) => void
}

export function TaskTable({ tasks, busyTaskId, onComplete }: TaskTableProps) {
  return (
    <div className="table-wrapper">
      <table className="task-table">
        <thead>
          <tr>
            <th scope="col">Title</th>
            <th scope="col">Status</th>
            <th scope="col">Priority</th>
            <th scope="col">Assignee</th>
            <th scope="col">Due</th>
            <th scope="col">
              <span className="visually-hidden">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <tr key={task.id}>
              <td>
                <Link to={`/tasks/${task.id}`}>{task.title}</Link>
              </td>
              <td>
                <StatusBadge status={task.status} />
              </td>
              <td>
                <PriorityBadge priority={task.priority} />
              </td>
              <td>{task.assignee?.name ?? <span className="muted">Unassigned</span>}</td>
              <td>{formatDate(task.dueDate)}</td>
              <td className="actions-cell">
                {task.status !== 'DONE' && (
                  <button
                    type="button"
                    className="button button-small"
                    disabled={busyTaskId === task.id}
                    aria-label={`Complete ${task.title}`}
                    onClick={() => onComplete(task)}
                  >
                    {busyTaskId === task.id ? 'Completing…' : 'Complete'}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
