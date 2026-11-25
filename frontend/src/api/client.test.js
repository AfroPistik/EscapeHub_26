import { test, expect, vi, afterEach } from 'vitest'
import { api, setUnauthorizedHandler, ApiError } from './client'

afterEach(() => vi.unstubAllGlobals())

function mockFetch(status, body) {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        status,
        ok: status >= 200 && status < 300,
        json: () => Promise.resolve(body),
    }))
}

test('το 401 καλεί τον handler λήξης session', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    mockFetch(401, { code: 'UNAUTHORIZED', message: 'Απαιτείται σύνδεση', fieldErrors: {} })

    await expect(api.get('/teams/mine')).rejects.toBeInstanceOf(ApiError)
    expect(handler).toHaveBeenCalledTimes(1)
})

test('με skip401 ο handler δεν καλείται', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    mockFetch(401, { code: 'UNAUTHORIZED', message: 'Απαιτείται σύνδεση', fieldErrors: {} })

    await expect(api.get('/users/me', { skip401: true })).rejects.toBeInstanceOf(ApiError)
    expect(handler).not.toHaveBeenCalled()
})

test('το 400 δίνει fieldErrors ως object', async () => {
    mockFetch(400, { code: 'VALIDATION_ERROR', message: 'Μη έγκυρα δεδομένα', fieldErrors: { email: 'Μη έγκυρο email' } })

    const err = await api.post('/auth/register', {}).catch(e => e)
    expect(err.status).toBe(400)
    expect(err.fieldErrors).toEqual({ email: 'Μη έγκυρο email' })
})

test('το 204 επιστρέφει null', async () => {
    mockFetch(204, null)
    await expect(api.post('/auth/logout')).resolves.toBeNull()
})