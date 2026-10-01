import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, buildQuery, request } from './client'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('builds a query string skipping empty values', () => {
    expect(buildQuery({ status: 'TODO', search: '', assigneeId: undefined, unassigned: false, page: 0 })).toBe(
      '?status=TODO&page=0',
    )
    expect(buildQuery({})).toBe('')
  })

  it('maps a problem detail response to an ApiError with field errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(400, {
          status: 400,
          detail: 'Validation failed',
          errors: [{ field: 'title', message: 'must not be blank' }],
        }),
      ),
    )

    const error = await request('/tasks', { method: 'POST', body: '{}' }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).message).toBe('Validation failed')
    expect((error as ApiError).fieldErrors).toEqual([{ field: 'title', message: 'must not be blank' }])
  })

  it('reports a network failure as a readable error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(request('/tasks')).rejects.toThrow('Unable to reach the server')
  })

  it('returns undefined for 204 responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

    await expect(request('/tasks/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
