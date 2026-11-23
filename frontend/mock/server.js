import express from 'express'

const app = express()
app.use(express.json())

const users = []
let current = null // ένας "συνδεδεμένος" χρήστης αρκεί για το mock

const fail = (res, status, code, message, fieldErrors = {}) =>
    res.status(status).json({ code, message, timestamp: new Date().toISOString(), fieldErrors })

app.post('/api/auth/register', (req, res) => {
    const { email = '', username = '', password = '' } = req.body
    const fieldErrors = []
    if (!/^\S+@\S+\.\S+$/.test(email)) fieldErrors.push({ field: 'email', message: 'Μη έγκυρο email' })
    if (username.length < 3 || username.length > 30) fieldErrors.push({ field: 'username', message: 'Το username πρέπει να έχει 3–30 χαρακτήρες' })
    if (password.length < 8) fieldErrors.push({ field: 'password', message: 'Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες' })
    if (fieldErrors.length) return fail(res, 400, 'VALIDATION_ERROR', 'Μη έγκυρα δεδομένα', fieldErrors)

    if (users.some(u => u.email.toLowerCase() === email.toLowerCase()))
        return fail(res, 409, 'EMAIL_TAKEN', 'Το email χρησιμοποιείται ήδη')
    if (users.some(u => u.username === username))
        return fail(res, 409, 'USERNAME_TAKEN', 'Το username χρησιμοποιείται ήδη')

    const user = { id: users.length + 1, email, username, password }
    users.push(user)
    res.status(201).json({ id: user.id, email, username })
})

app.post('/api/auth/login', (req, res) => {
    const { email = '', password = '' } = req.body
    const user = users.find(u => u.email.toLowerCase() === email.toLowerCase() && u.password === password)
    if (!user) return fail(res, 401, 'BAD_CREDENTIALS', 'Λάθος email ή κωδικός')
    current = user
    res.json({ id: user.id, email: user.email, username: user.username })
})

app.get('/api/users/me', (req, res) => {
    if (!current) return fail(res, 401, 'UNAUTHORIZED', 'Απαιτείται σύνδεση')
    res.json({ id: current.id, email: current.email, username: current.username })
})

app.post('/api/auth/logout', (req, res) => { current = null; res.status(204).end() })

app.listen(4010, () => console.log('Mock API στο http://localhost:4010'))