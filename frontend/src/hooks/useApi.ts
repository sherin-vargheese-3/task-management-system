import { useCallback, useEffect, useState } from 'react'

type Fetcher<T> = (signal: AbortSignal) => Promise<T>

interface ApiState<T> {
  data: T | null
  error: Error | null
  settledFetcher: Fetcher<T> | null
  settledToken: number
}

export function useApi<T>(fetcher: Fetcher<T>) {
  const [reloadToken, setReloadToken] = useState(0)
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    error: null,
    settledFetcher: null,
    settledToken: -1,
  })

  useEffect(() => {
    const controller = new AbortController()
    fetcher(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setState({ data, error: null, settledFetcher: fetcher, settledToken: reloadToken })
        }
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setState((previous) => ({
            data: previous.data,
            error: error instanceof Error ? error : new Error('Request failed'),
            settledFetcher: fetcher,
            settledToken: reloadToken,
          }))
        }
      })
    return () => controller.abort()
  }, [fetcher, reloadToken])

  const loading = state.settledFetcher !== fetcher || state.settledToken !== reloadToken
  const reload = useCallback(() => setReloadToken((token) => token + 1), [])
  const setData = useCallback(
    (data: T) => setState((previous) => ({ ...previous, data, error: null })),
    [],
  )

  return { data: state.data, loading, error: loading ? null : state.error, reload, setData }
}
