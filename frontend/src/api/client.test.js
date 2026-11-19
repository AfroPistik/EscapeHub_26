import { api, setUnauthorizedHandler, ApiError } from './client'

describe('API Client - 401 Unauthorized handling', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    test('καλεί τον unauthorized handler και πετάει ApiError όταν λαμβάνει 401 χωρίς skip401', async () => {
        const handler = vi.fn()
        setUnauthorizedHandler(handler)

        // Mock global fetch να επιστρέφει status 401
        global.fetch = vi.fn().mockResolvedValue({
            status: 401,
            ok: false,
            json: async () => ({ message: 'Unauthorized', code: 'UNAUTHORIZED' }),
        })

        await expect(api.get('/users/me')).rejects.toThrow(ApiError)
        expect(handler).toHaveBeenCalledTimes(1)
    })
})