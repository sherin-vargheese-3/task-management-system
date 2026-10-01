interface ErrorMessageProps {
  error: Error | string
  onRetry?: () => void
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="feedback" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      {label}
    </div>
  )
}

export function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
  const message = typeof error === 'string' ? error : error.message
  return (
    <div className="feedback feedback-error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button type="button" className="button button-secondary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  )
}

export function EmptyState({ message }: { message: string }) {
  return <div className="feedback feedback-empty">{message}</div>
}
