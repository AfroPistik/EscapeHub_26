export function validateRegister({ email, username, password }) {
    const errors = {}
    if (!/^\S+@\S+\.\S+$/.test(email)) errors.email = 'Μη έγκυρο email'
    if (username.length < 3 || username.length > 30) errors.username = 'Το username πρέπει να έχει 3–30 χαρακτήρες'
    if (password.length < 8) errors.password = 'Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες'
    return errors
}

// Μετατρέπει τα fieldErrors του backend σε αντικείμενο { πεδίο: μήνυμα }
export const fieldErrorsToObject = (list = []) =>
    Object.fromEntries(list.map(e => [e.field, e.message]))