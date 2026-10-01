import { useCallback } from 'react'
import { usersApi } from '../api/users'
import { useApi } from './useApi'

export function useUsers() {
  const fetcher = useCallback((signal: AbortSignal) => usersApi.list(signal), [])
  const { data, loading, error } = useApi(fetcher)
  return { users: data?.content ?? [], loading, error }
}
