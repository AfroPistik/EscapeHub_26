import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { api, setUnauthorizedHandler } from '../api/client'

const AuthContext = createContext(null)
// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [loading, setLoading] = useState(true)
    const [sessionExpired, setSessionExpired] = useState(false)
    const userRef = useRef(null)

    // Κρατά πάντα την τρέχουσα τιμή του χρήστη για τον handler του 401
    useEffect(() => {
        userRef.current = user
    }, [user])

    useEffect(() => {
        api.get('/users/me', { skip401: true })
            .then(setUser)
            .catch(() => setUser(null))
            .finally(() => setLoading(false))

        setUnauthorizedHandler(() => {
            if (userRef.current) setSessionExpired(true) // μήνυμα μόνο αν ήταν συνδεδεμένος
            setUser(null)
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