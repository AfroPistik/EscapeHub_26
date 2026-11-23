export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || 'Κάτι πήγε στραβά')
        this.status = status
        this.code = body?.code
        this.fieldErrors = body?.fieldErrors || {}    }
}

let onUnauthorized = () => {}
export const setUnauthorizedHandler = fn => { onUnauthorized = fn }

async function request(method, path, body, { skip401 = false } = {}) {
    const res = await fetch(`/api${path}`, {
        method,
        credentials: 'include', // στέλνει το session cookie
        headers: body ? { 'Content-Type': 'application/json' } : undefined,
        body: body ? JSON.stringify(body) : undefined,
    })
    if (res.status === 204) return null
    const data = await res.json().catch(() => null)
    if (!res.ok) {
        if (res.status === 401 && !skip401) onUnauthorized()
        throw new ApiError(res.status, data)
    }
    return data
}

export const api = {
    get: (path, opts) => request('GET', path, undefined, opts),
    post: (path, body, opts) => request('POST', path, body, opts),
    delete: (path, opts) => request('DELETE', path, undefined, opts),
}