import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
    const { login, sessionExpired } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const [form, setForm] = useState({ email: '', password: '' })
    const [error, setError] = useState('')

    const onSubmit = async e => {
        e.preventDefault()
        setError('')
        if (!form.email || !form.password) return setError('Συμπλήρωσε email και κωδικό')
        try {
            await login(form.email, form.password)
            navigate(location.state?.from?.pathname || '/teams', { replace: true })
        } catch (err) {
            setError(err.message)
        }
    }

    return (
        <form onSubmit={onSubmit} noValidate>
            <h1>Είσοδος</h1>
            {location.state?.registered && <p>Η εγγραφή ολοκληρώθηκε. Συνδέσου.</p>}
            {sessionExpired && <p role="alert">Η σύνδεσή σου έληξε. Συνδέσου ξανά.</p>}
            {error && <p role="alert">{error}</p>}
            <label htmlFor="email">Email</label>
            <input id="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
            <label htmlFor="password">Κωδικός</label>
            <input id="password" type="password" value={form.password}
                   onChange={e => setForm({ ...form, password: e.target.value })} />
            <button type="submit">Είσοδος</button>
            <p>Δεν έχεις λογαριασμό; <Link to="/register">Εγγραφή</Link></p>
        </form>
    )
}