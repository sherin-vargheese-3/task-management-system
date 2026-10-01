import type { Page, User } from '../types'
import { request } from './client'

export const usersApi = {
  list: (signal?: AbortSignal) => request<Page<User>>('/users?size=100&sort=name,asc', { signal }),
}
