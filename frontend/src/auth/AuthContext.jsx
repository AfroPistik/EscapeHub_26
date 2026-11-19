import { createContext, useContext, useEffect, useState } from 'react'
import { api, setUnauthorizedHandler } from '../api/client'

const AuthContext = createContext(null)

export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [loading, setLoading] = useState(true)
    const [sessionExpired, setSessionExpired] = useState(false)

    useEffect(() => {
        api.get('/users/me', { skip401: true })
            .then(setUser)
            .catch(() => setUser(null))
            .finally(() => setLoading(false))

        setUnauthorizedHandler(() => {
            setUser(prev => {
                if (prev) setSessionExpired(true) // μήνυμα μόνο αν ήταν συνδεδεμένος
                return null
            })
        })
    }, [])

    const login = async (email, password) => {
        const u = await api.post('/auth/login', { email, password }, { skip401: true })
        setSessionExpired(false)
        setUser(u)
    }

    const register = data => api.post('/auth/register', data)

    const logout = async () => {
        await api.post('/auth/logout')
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, loading, sessionExpired, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    )
}