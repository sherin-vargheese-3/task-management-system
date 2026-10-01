import { useNavigate } from 'react-router-dom'
import { tasksApi } from '../api/tasks'
import { ErrorMessage } from '../components/Feedback/Feedback'
import { TaskForm } from '../components/TaskForm/TaskForm'
import { useUsers } from '../hooks/useUsers'
import type { TaskPayload } from '../types'

export function CreateTaskPage() {
  const navigate = useNavigate()
  const { users, error: usersError } = useUsers()

  const handleSubmit = async (payload: TaskPayload) => {
    const created = await tasksApi.create(payload)
    navigate(`/tasks/${created.id}`)
  }

  return (
    <div className="page page-narrow">
      <header className="page-header">
        <h1>New task</h1>
      </header>
      {usersError && <ErrorMessage error={`Could not load users: ${usersError.message}`} />}
      <TaskForm users={users} submitLabel="Create task" onSubmit={handleSubmit} onCancel={() => navigate('/')} />
    </div>
  )
}
