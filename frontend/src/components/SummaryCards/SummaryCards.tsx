import type { TaskSummary } from '../../types'

export function SummaryCards({ summary }: { summary: TaskSummary }) {
  const cards = [
    { label: 'Total', value: summary.total },
    { label: 'To do', value: summary.todo },
    { label: 'In progress', value: summary.inProgress },
    { label: 'Done', value: summary.done },
  ]
  return (
    <section className="summary" aria-label="Task summary">
      {cards.map((card) => (
        <div key={card.label} className="summary-card">
          <span className="summary-value">{card.value}</span>
          <span className="summary-label">{card.label}</span>
        </div>
      ))}
    </section>
  )
}
