import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { validateRegister, fieldErrorsToObject } from '../auth/validation'

export default function RegisterPage() {
    const { register } = useAuth()
    const navigate = useNavigate()
    const [form, setForm] = useState({ email: '', username: '', password: '' })
    const [errors, setErrors] = useState({})
    const [general, setGeneral] = useState('')

    const onChange = e => setForm({ ...form, [e.target.name]: e.target.value })

    const onSubmit = async e => {
        e.preventDefault()
        setGeneral('')
        const clientErrors = validateRegister(form)
        setErrors(clientErrors)
        if (Object.keys(clientErrors).length) return
        try {
            await register(form)
            navigate('/login', { state: { registered: true } })
        } catch (err) {
            if (err.status === 400) setErrors(fieldErrorsToObject(err.fieldErrors))
            else if (err.code === 'EMAIL_TAKEN') setErrors({ email: err.message })
            else if (err.code === 'USERNAME_TAKEN') setErrors({ username: err.message })
            else setGeneral(err.message)
        }
    }

    return (
        <form onSubmit={onSubmit} noValidate>
            <h1>Εγγραφή</h1>
            {general && <p role="alert">{general}</p>}
            {['email', 'username', 'password'].map(name => (
                <div key={name}>
                    <label htmlFor={name}>{{ email: 'Email', username: 'Username', password: 'Κωδικός' }[name]}</label>
                    <input id={name} name={name} type={name === 'password' ? 'password' : 'text'}
                           value={form[name]} onChange={onChange} />
                    {errors[name] && <span role="alert">{errors[name]}</span>}
                </div>
            ))}
            <button type="submit">Εγγραφή</button>
            <p>Έχεις ήδη λογαριασμό; <Link to="/login">Είσοδος</Link></p>
        </form>
    )
}