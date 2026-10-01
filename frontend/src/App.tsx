import { Link, NavLink, Route, Routes } from 'react-router-dom'
import { CreateTaskPage } from './pages/CreateTaskPage'
import { DashboardPage } from './pages/DashboardPage'
import { TaskDetailsPage } from './pages/TaskDetailsPage'

function NotFoundPage() {
  return (
    <div className="page page-narrow">
      <h1>Page not found</h1>
      <Link to="/">Back to dashboard</Link>
    </div>
  )
}

export default function App() {
  return (
    <>
      <nav className="topbar">
        <Link to="/" className="brand">
          Task Manager
        </Link>
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        <NavLink to="/tasks/new">New task</NavLink>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/tasks/new" element={<CreateTaskPage />} />
          <Route path="/tasks/:id" element={<TaskDetailsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </>
  )
}
