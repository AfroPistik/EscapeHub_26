import express from 'express'

const app = express()
app.use(express.json())

const users = []
let current = null // ένας "συνδεδεμένος" χρήστης αρκεί για το mock

const fail = (res, status, code, message, fieldErrors = {}) =>
    res.status(status).json({
        code,
        message,
        timestamp: new Date().toISOString(),
        fieldErrors,
    })

// Εγγραφή
app.post('/api/auth/register', (req, res) => {
    const { email = '', username = '', password = '' } = req.body

    const fieldErrors = {}
    if (!/^\S+@\S+\.\S+$/.test(email)) {
        fieldErrors.email = 'Μη έγκυρο email'
    }
    if (username.length < 3 || username.length > 30) {
        fieldErrors.username = 'Το username πρέπει να έχει 3–30 χαρακτήρες'
    }
    if (password.length < 8) {
        fieldErrors.password = 'Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες'
    }
    if (Object.keys(fieldErrors).length) {
        return fail(res, 400, 'VALIDATION_ERROR', 'Μη έγκυρα δεδομένα', fieldErrors)
    }

    if (users.some(u => u.email.toLowerCase() === email.toLowerCase())) {
        return fail(res, 409, 'EMAIL_TAKEN', 'Το email χρησιμοποιείται ήδη')
    }
    if (users.some(u => u.username === username)) {
        return fail(res, 409, 'USERNAME_TAKEN', 'Το username χρησιμοποιείται ήδη')
    }

    const user = { id: users.length + 1, email, username, password }
    users.push(user)
    res.status(201).json({ id: user.id, email, username })
})

// Είσοδος
app.post('/api/auth/login', (req, res) => {
    const { email = '', password = '' } = req.body
    const user = users.find(
        u => u.email.toLowerCase() === email.toLowerCase() && u.password === password
    )
    if (!user) {
        return fail(res, 401, 'INVALID_CREDENTIALS', 'Λάθος email ή κωδικός')
    }
    current = user
    res.json({ id: user.id, email: user.email, username: user.username })
})

// Τρέχων χρήστης
app.get('/api/users/me', (req, res) => {
    if (!current) {
        return fail(res, 401, 'UNAUTHORIZED', 'Απαιτείται σύνδεση')
    }
    res.json({ id: current.id, email: current.email, username: current.username })
})

// Έξοδος
app.post('/api/auth/logout', (req, res) => {
    current = null
    res.status(204).end()
})

const teams = []
let nextTeamId = 1

const INVITE_CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

function generateInviteCode(length = 8) {
  let code = ''
  for (let i = 0; i < length; i++) {
    const idx = Math.floor(Math.random() * INVITE_CODE_CHARS.length)
    code += INVITE_CODE_CHARS[idx]
  }
  return code
}

function requireAuth(req, res, next) {
  if (!current) {
    return fail(res, 401, 'UNAUTHORIZED', 'Απαιτείται σύνδεση')
  }
  next()
}

// Λίστα ομάδων του συνδεδεμένου χρήστη με ρόλο
app.get('/api/teams/mine', requireAuth, (req, res) => {
  const myTeams = teams
    .filter(t => t.members.some(m => m.userId === current.id))
    .map(t => {
      const membership = t.members.find(m => m.userId === current.id)
      return {
        id: t.id,
        name: t.name,
        role: membership.role,
      }
    })
  res.json(myTeams)
})

// Δημιουργία νέας ομάδας (3-50 χαρακτήρες)
app.post('/api/teams', requireAuth, (req, res) => {
  const rawName = typeof req.body?.name === 'string' ? req.body.name : ''
  const trimmedName = rawName.trim()

  const fieldErrors = {}
  if (trimmedName.length < 3 || trimmedName.length > 50) {
    fieldErrors.name = 'Το όνομα ομάδας πρέπει να είναι από 3 έως 50 χαρακτήρες'
    return fail(res, 400, 'VALIDATION_ERROR', 'Μη έγκυρα δεδομένα ομάδας', fieldErrors)
  }

  const inviteCode = generateInviteCode(8)
  const team = {
    id: nextTeamId++,
    name: trimmedName,
    inviteCode,
    createdAt: new Date().toISOString(),
    members: [
      {
        userId: current.id,
        username: current.username,
        role: 'OWNER',
        joinedAt: new Date().toISOString(),
      },
    ],
  }
  teams.push(team)

  res.status(201).json({
    id: team.id,
    name: team.name,
    inviteCode: team.inviteCode,
    createdAt: team.createdAt,
  })
})

// Λεπτομέρειες ομάδας
app.get('/api/teams/:id', requireAuth, (req, res) => {
  const teamId = Number(req.params.id)
  const team = teams.find(t => t.id === teamId)
  if (!team) {
    return fail(res, 404, 'TEAM_NOT_FOUND', 'Η ομάδα δεν βρέθηκε')
  }
  const membership = team.members.find(m => m.userId === current.id)
  if (!membership) {
    return fail(res, 403, 'NOT_A_MEMBER', 'Δεν είστε μέλος αυτής της ομάδας')
  }
  res.json({
    id: team.id,
    name: team.name,
    inviteCode: team.inviteCode,
    createdAt: team.createdAt,
  })
})

app.listen(4010, () => console.log('Mock API στο http://localhost:4010'))
